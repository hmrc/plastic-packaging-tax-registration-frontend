/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.partner

import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import controllers.partner.{routes => partnerRoutes}
import controllers.{routes => commonRoutes}
import forms.contact.JobTitle
import models.genericregistration.{Partner, PartnerContactDetails}
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.partner.partner_job_title_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerJobTitleController @Inject() (
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partner_job_title_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Cacheable
    with I18nSupport {

  def displayNewPartner(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.inflightPartner.map { partner =>
        renderPageFor(
          partner,
          partnerRoutes.PartnerContactNameController.displayNewPartner,
          partnerRoutes.PartnerJobTitleController.submitNewPartner()
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.findPartner(partnerId).map { partner =>
        renderPageFor(
          partner,
          partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
          partnerRoutes.PartnerJobTitleController.submitExistingPartner(partnerId)
        )
      }.getOrElse(throw new IllegalStateException("Expected existing partner missing"))
    }

  private def renderPageFor(partner: Partner, backCall: Call, submitCall: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Result =
    partner.contactDetails.map { contactDetails =>
      contactDetails.name.map { contactName =>
        val form = contactDetails.jobTitle match {
          case Some(data) =>
            JobTitle.form().fill(JobTitle(data))
          case None =>
            JobTitle.form()
        }
        Ok(page(form, contactName, submitCall))
      }.getOrElse(throw new IllegalStateException("Expected partner contact name missing"))
    }.getOrElse(throw new IllegalStateException("Expected partner contact details missing"))

  def submitNewPartner(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      request.registration.inflightPartner.map { partner =>
        handleSubmission(
          partner,
          partnerRoutes.PartnerTypeController.displayNewPartner(),
          partnerRoutes.PartnerJobTitleController.submitNewPartner(),
          partnerRoutes.PartnerEmailAddressController.displayNewPartner(),
          commonRoutes.TaskListController.displayPage(),
          updateInflightPartner
        )
      }.getOrElse(Future.successful(throw new IllegalStateException("Expected inflight partner missing")))
    }

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      def updateAction(jobTitle: JobTitle): Future[Either[ServiceError, Registration]] =
        updateExistingPartner(jobTitle, partnerId)

      request.registration.findPartner(partnerId).map { partner =>
        handleSubmission(
          partner,
          partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
          partnerRoutes.PartnerJobTitleController.submitExistingPartner(partnerId),
          routes.PartnerEmailAddressController.displayExistingPartner(partnerId),
          partnerRoutes.PartnerJobTitleController.displayExistingPartner(partnerId),
          updateAction
        )
      }.getOrElse(Future.successful(throw new IllegalStateException("Expected existing partner missing")))
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
          partner.contactDetails.flatMap(_.name).map { contactName =>
            Future.successful(BadRequest(page(formWithErrors, contactName, submitCall)))
          }.getOrElse(Future.successful(throw new IllegalStateException("Expected partner contact name missing"))),
        jobTitle =>
          updateAction(jobTitle).map {
            case Right(_)    => Redirect(onwardsCall)
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
    val withJobTitle = partner.contactDetails.getOrElse(PartnerContactDetails()).copy(jobTitle = Some(formData.value))
    partner.copy(contactDetails = Some(withJobTitle))
  }

}
