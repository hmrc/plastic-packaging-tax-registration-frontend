/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.contact

import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import forms.contact.PhoneNumber
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.contact.phone_number_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsTelephoneNumberController @Inject() (
                                                          journeyAction: JourneyAction,
                                                          override val registrationConnector: RegistrationConnector,
                                                          mcc: MessagesControllerComponents,
                                                          page: phone_number_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.primaryContactDetails.phoneNumber match {
        case Some(data) =>
          Ok(buildPage(PhoneNumber.form().fill(PhoneNumber(data))))
        case _ =>
          Ok(buildPage(PhoneNumber.form()))
      }
    }

  def submit(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            Future.successful(
              BadRequest(buildPage(formWithErrors))
            ),
          phoneNumber =>
            updateRegistration(phoneNumber).map {
              case Right(_) => Redirect(routes.ContactDetailsConfirmAddressController.displayPage())
              case Left(error) => throw error
            }
        )
    }

  private def buildPage
  (
    form: Form[PhoneNumber]
  )(implicit request: JourneyRequest[AnyContent]) =
    page(
      form,
      routes.ContactDetailsTelephoneNumberController.submit(),
      request.registration.isGroup
    )

  private def updateRegistration(
    formData: PhoneNumber
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val withUpdatedPhoneNumber =
        registration.primaryContactDetails.copy(phoneNumber = Some(formData.value))
      registration.copy(primaryContactDetails = withUpdatedPhoneNumber)
    }

}
