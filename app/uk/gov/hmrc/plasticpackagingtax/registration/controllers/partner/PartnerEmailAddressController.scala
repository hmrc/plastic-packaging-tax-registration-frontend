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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_email_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerEmailAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: partner_email_address_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.inflightPartner.map { partner =>
        renderPageFor(partner,
                      partnerRoutes.PartnerContactNameController.displayPage(),
                      partnerRoutes.PartnerEmailAddressController.submit()
        )

      }.getOrElse(throw new IllegalStateException("Expected inflight partner missing"))
    }

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.findPartner(partnerId).map { partner =>
        renderPageFor(partner,
                      partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
                      partnerRoutes.PartnerEmailAddressController.submitExistingPartner(partnerId)
        )
      }.getOrElse(throw new IllegalStateException("Expected existing partner missing"))
    }

  private def renderPageFor(partner: Partner, backCall: Call, submitCall: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Result =
    partner.contactDetails.map { contactDetails =>
      contactDetails.name.map { contactName =>
        val form = contactDetails.emailAddress match {
          case Some(data) =>
            EmailAddress.form().fill(EmailAddress(data))
          case _ =>
            EmailAddress.form()
        }
        Ok(page(form, backCall, submitCall, contactName))
      }.getOrElse(throw new IllegalStateException("Expected partner contact name missing"))
    }.getOrElse(throw new IllegalStateException("Expected partner contact details missing"))

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.inflightPartner.map { partner: Partner =>
        handleSubmission(partner,
                         partnerRoutes.PartnerContactNameController.displayPage(),
                         partnerRoutes.PartnerEmailAddressController.submit(),
                         partnerRoutes.PartnerPhoneNumberController.displayPage(),
                         commonRoutes.TaskListController.displayPage(),
                         updateInflightRegistration
        )
      }.getOrElse(throw new IllegalStateException("Expected inflight partner missing"))
    }

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      def updateAction(emailAddress: EmailAddress): Future[Either[ServiceError, Registration]] =
        updateExistingPartner(emailAddress, partnerId)

      request.registration.findPartner(partnerId).map { partner =>
        handleSubmission(partner,
                         partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(
                           partnerId
                         ),
                         partnerRoutes.PartnerEmailAddressController.submitExistingPartner(
                           partnerId
                         ),
                         routes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
                         partnerRoutes.PartnerEmailAddressController.displayExistingPartner(
                           partnerId
                         ),
                         updateAction
        )

      }.getOrElse(throw new IllegalStateException("Expected existing partner missing"))
    }

  private def handleSubmission(
    partner: Partner,
    backCall: Call,
    submitCall: Call,
    onwardCall: Call,
    dropoutCall: Call,
    updateAction: EmailAddress => Future[Either[ServiceError, Registration]]
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] =
    EmailAddress.form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[EmailAddress]) =>
          partner.contactDetails.flatMap(_.name).map {
            contactName =>
              Future.successful(BadRequest(page(formWithErrors, backCall, submitCall, contactName)))
          }.getOrElse(
            Future.successful(
              throw new IllegalStateException("Expected partner contact name missing")
            )
          ),
        emailAddress =>
          updateAction(emailAddress).map {
            case Right(_) =>
              FormAction.bindFromRequest match {
                case SaveAndContinue =>
                  Redirect(onwardCall)
                case _ =>
                  Redirect(dropoutCall)
              }
            case Left(error) =>
              throw error

          }
      )

  private def updateInflightRegistration(
    formData: EmailAddress
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.inflightPartner.map { partner =>
        val withEmailAddress =
          partner.contactDetails.map { contactDetails =>
            val updatedContactDetailsWithEmailAddress =
              contactDetails.copy(emailAddress = Some(formData.value))
            partner.copy(contactDetails = Some(updatedContactDetailsWithEmailAddress))
          }
        registration.withInflightPartner(withEmailAddress)
      }.getOrElse {
        registration
      }
    }

  private def updateExistingPartner(formData: EmailAddress, partnerId: String)(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.withUpdatedPartner(
        partnerId,
        partner =>
          partner.copy(contactDetails =
            partner.contactDetails.map(_.copy(emailAddress = Some(formData.value)))
          )
      )
    }

}
