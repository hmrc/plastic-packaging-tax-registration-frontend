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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partnership

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partnership.{
  routes => partnershipRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.PartnerContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.email_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipOtherPartnerEmailAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: email_address_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      (for {
        inflight       <- request.registration.inflightPartner
        contactDetails <- inflight.contactDetails
        contactName    <- contactDetails.name

      } yield contactDetails.emailAddress match {
        case Some(data) =>
          Ok(
            page(EmailAddress.form().fill(EmailAddress(data)),
                 partnershipRoutes.PartnershipOtherPartnerContactNameController.displayPage(),
                 partnershipRoutes.PartnershipOtherPartnerEmailAddressController.submit(),
                 contactName
            )
          )
        case _ =>
          Ok(
            page(EmailAddress.form(),
                 partnershipRoutes.PartnershipOtherPartnerContactNameController.displayPage(),
                 partnershipRoutes.PartnershipOtherPartnerEmailAddressController.submit(),
                 contactName
            )
          )
      }).getOrElse(NotFound)
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            (for {
              inflight       <- request.registration.inflightPartner
              contactDetails <- inflight.contactDetails
              contactName    <- contactDetails.name

            } yield Future.successful(
              BadRequest(
                page(formWithErrors,
                     partnershipRoutes.PartnershipOtherPartnerContactNameController.displayPage(),
                     partnershipRoutes.PartnershipOtherPartnerEmailAddressController.submit(),
                     contactName
                )
              )
            )).getOrElse(Future.successful(NotFound)),
          emailAddress =>
            updateRegistration(emailAddress).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    Redirect(
                      partnershipRoutes.PartnershipOtherPartnerPhoneNumberController.displayPage()
                    )
                  case _ =>
                    Redirect(commonRoutes.TaskListController.displayPage())
                }
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: EmailAddress
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      (for {
        inflight <- registration.inflightPartner
      } yield {
        val updatedContactDetails =
          inflight.contactDetails.getOrElse(PartnerContactDetails()).copy(emailAddress =
            Some(formData.value)
          )
        inflight.copy(contactDetails = Some(updatedContactDetails))

      }).map { updated =>
        registration.withInflightPartner(Some(updated))
      }.getOrElse {
        registration
      }
    }

}
