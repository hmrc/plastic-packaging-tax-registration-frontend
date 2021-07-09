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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  EmailVerificationConnector,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{AuthAction, FormAction}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.EmailAddressPasscode
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.VerifyPasscodeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.email_address_passcode_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsEmailAddressPasscodeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  emailVerificationConnector: EmailVerificationConnector,
  page: email_address_passcode_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(page(EmailAddressPasscode.form(), request.registration.primaryContactDetails.email))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      EmailAddressPasscode.form()
        .bindFromRequest()
        .fold((formWithErrors: Form[EmailAddressPasscode]) => {
                println(formWithErrors.errors)
                Future.successful(BadRequest(page(formWithErrors, None)))
              },
              emailAddressPasscode =>
                FormAction.bindFromRequest match {
                  case uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.Continue =>
                    continue(emailAddressPasscode.value,
                             request.registration.primaryContactDetails.email
                    )
                  case _ =>
                    Future(Redirect(routes.RegistrationController.displayPage()))
                }
        )
    }

  private def continue(passcode: String, email: Option[String])(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Result] =
    createPassCodeVerficationRequest(passcode,
                                     email.get,
                                     request.registration.primaryContactDetails.journeyId.get
    ).flatMap {
      case Right("complete") =>
        Future(
          Redirect(routes.ContactDetailsEmailAddressPasscodeConfirmationController.displayPage())
        )
      case Right("incorrectPasscode") =>
        Future.successful(
          BadRequest(
            page(EmailAddressPasscode.form().withError("incorrectPasscode", "Incorrect Passcode"),
                 None
            )
          )
        )
      case Right("tooManyAttempts") =>
        Future.successful(
          BadRequest(
            page(EmailAddressPasscode.form().withError("tooManyAttempts", "Too Many Attempts"),
                 None
            )
          )
        )
      case Right("journeyNotFound") =>
        Future.successful(
          BadRequest(
            page(EmailAddressPasscode.form().withError("journeyNotFound",
                                                       "Passcode for email address is not found"
                 ),
                 None
            )
          )
        )
      case Left(error) => throw error
    }

  private def createPassCodeVerficationRequest(passcode: String, email: String, journeyId: String)(
    implicit hc: HeaderCarrier
  ): Future[Either[ServiceError, String]] =
    emailVerificationConnector.verifyPasscode(
      journeyId = journeyId,
      VerifyPasscodeRequest(passcode = passcode, email = email)
    )

}
