/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PhoneNumber
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.phone_number_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsTelephoneNumberController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: phone_number_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.primaryContactDetails.phoneNumber match {
        case Some(data) =>
          Ok(page(PhoneNumber.form().fill(PhoneNumber(data))))
        case _ => Ok(page(PhoneNumber.form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold((formWithErrors: Form[PhoneNumber]) => {
                println(formWithErrors)
                Future.successful(BadRequest(page(formWithErrors)))
              },
              phoneNumber =>
                updateRegistration(phoneNumber).map {
                  case Right(_) =>
                    FormAction.bindFromRequest match {
                      case SaveAndContinue =>
                        Redirect(routes.ContactDetailsAddressController.displayPage())
                      case _ =>
                        Redirect(routes.RegistrationController.displayPage())
                    }
                  case Left(error) => throw error
                }
        )
    }

  private def updateRegistration(
    formData: PhoneNumber
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val withUpdatedPhoneNumber =
        registration.primaryContactDetails.copy(phoneNumber = Some(formData.value))
      registration.copy(primaryContactDetails = withUpdatedPhoneNumber)
    }

}
