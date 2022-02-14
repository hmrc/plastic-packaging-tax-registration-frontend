/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.JobTitle
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  Partner,
  PartnerContactDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_job_title_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerJobTitleController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partner_job_title_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayNewPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.inflightPartner.map { partner =>
        renderPageFor(partner,
                      partnerRoutes.PartnerTypeController.displayNewPartner(),
                      partnerRoutes.PartnerJobTitleController.submitNewPartner()
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.findPartner(partnerId).map { partner =>
        renderPageFor(partner,
                      partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
                      partnerRoutes.PartnerJobTitleController.submitExistingPartner(partnerId)
        )
      }.getOrElse(throw new IllegalStateException("Expected existing partner missing"))
    }

  private def renderPageFor(partner: Partner, backCall: Call, submitCall: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Result = {
    val existingNameFields = for {
      contactDetails <- partner.contactDetails
      jobTitle       <- contactDetails.jobTitle
    } yield jobTitle

    val form = existingNameFields match {
      case Some(data) =>
        JobTitle.form().fill(JobTitle(data))
      case None =>
        JobTitle.form()
    }

    val value = partner.contactDetails.get.name.get // TODO naked get
    Ok(page(form, value, backCall, submitCall))
  }

  def submitNewPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.inflightPartner.map { partner =>
        handleSubmission(partner,
                         partnerRoutes.PartnerTypeController.displayNewPartner(),
                         partnerRoutes.PartnerContactNameController.submitNewPartner(),
                         partnerRoutes.PartnerEmailAddressController.displayNewPartner(),
                         commonRoutes.TaskListController.displayPage(),
                         updateInflightPartner
        )
      }.getOrElse(
        Future.successful(throw new IllegalStateException("Expected inflight partner missing"))
      )
    }

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      def updateAction(jobTitle: JobTitle): Future[Either[ServiceError, Registration]] =
        updateExistingPartner(jobTitle, partnerId)

      request.registration.findPartner(partnerId).map { partner =>
        handleSubmission(partner,
                         partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(
                           partnerId
                         ),
                         partnerRoutes.PartnerContactNameController.submitExistingPartner(
                           partnerId
                         ),
                         routes.PartnerEmailAddressController.displayExistingPartner(partnerId),
                         partnerRoutes.PartnerContactNameController.displayExistingPartner(
                           partnerId
                         ),
                         updateAction
        )
      }.getOrElse(
        Future.successful(throw new IllegalStateException("Expected existing partner missing"))
      )
    }

  private def handleSubmission(
    partner: Partner,
    backCall: Call,
    submitCall: Call,
    onwardsCall: Call,
    dropoutCall: Call,
    updateAction: JobTitle => Future[Either[ServiceError, Registration]]
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    JobTitle.form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[JobTitle]) =>
          Future.successful(BadRequest(page(formWithErrors, partner.name, backCall, submitCall))),
        jobTitle =>
          updateAction(jobTitle).map {
            case Right(_) =>
              FormAction.bindFromRequest match {
                case SaveAndContinue =>
                  Redirect(onwardsCall)
                case _ =>
                  Redirect(dropoutCall)
              }
            case Left(error) => throw error
          }
      )

  private def updateInflightPartner(
    formData: JobTitle
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.inflightPartner.map { partner =>
        registration.withInflightPartner(Some(withJobTitle(partner, formData)))
      }.getOrElse {
        registration
      }
    }

  private def updateExistingPartner(formData: JobTitle, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.withUpdatedPartner(partnerId, partner => withJobTitle(partner, formData))
    }

  private def withJobTitle(partner: Partner, formData: JobTitle) = {
    val withJobTitle = partner.contactDetails.getOrElse(PartnerContactDetails()).copy(jobTitle =
      Some(formData.value)
    )
    partner.copy(contactDetails = Some(withJobTitle))
  }

}
