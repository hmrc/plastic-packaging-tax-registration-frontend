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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.PhoneNumber
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.phone_number_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerPhoneNumberController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: phone_number_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.partnershipNominatedPartner.flatMap(_.contactDetails).map {
        contactDetails =>
          contactDetails.name.map { contactName =>
            val form = contactDetails.phoneNumber match {
              case Some(data) =>
                PhoneNumber.form().fill(PhoneNumber(data))
              case _ =>
                PhoneNumber.form()
            }

            Ok(
              page(form,
                   partnerRoutes.PartnerContactNameController.displayPage(),
                   partnerRoutes.PartnerPhoneNumberController.submit(),
                   contactName
              )
            )
          }.getOrElse(throw new IllegalStateException("Expected partner contact details missing"))
      }.getOrElse(throw new IllegalStateException("Expected partner name missing"))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            request.registration.inflightPartner.flatMap(_.contactDetails.flatMap(_.name)).map {
              contactName =>
                Future.successful(
                  BadRequest(
                    page(formWithErrors,
                         partnerRoutes.PartnerContactNameController.displayPage(),
                         partnerRoutes.PartnerPhoneNumberController.submit(),
                         contactName
                    )
                  )
                )
            }.getOrElse(
              Future.successful(throw new IllegalStateException("Expected partner name missing"))
            ),
          phoneNumber =>
            updateRegistration(phoneNumber).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    Redirect(partnerRoutes.PartnerCheckAnswersController.nominatedPartner())
                  case _ =>
                    Redirect(commonRoutes.TaskListController.displayPage())
                }
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: PhoneNumber
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.partnershipNominatedPartner.map { partner =>
        val updatedPartnerWithPhoneNumber: Option[Partner] =
          partner.contactDetails.map { contactDetails =>
            val updatedContactDetailsWithPhoneNumber =
              contactDetails.copy(phoneNumber = Some(formData.value))
            partner.copy(contactDetails = Some(updatedContactDetailsWithPhoneNumber))
          }
        registration.withPartnershipNominatedPartner(updatedPartnerWithPhoneNumber)
      }.getOrElse {
        registration
      }
    }

}
