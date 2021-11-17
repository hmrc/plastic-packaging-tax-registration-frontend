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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  EmailVerificationConnector,
  RegistrationConnector,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  Continue => ContinueAction
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddressPasscode
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus.{
  COMPLETE,
  INCORRECT_PASSCODE,
  JourneyStatus,
  TOO_MANY_ATTEMPTS
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  EmailStatus,
  VerifyPasscodeRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.email_address_passcode_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsEmailAddressPasscodeController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  emailVerificationConnector: EmailVerificationConnector,
  override val registrationConnector: RegistrationConnector,
  page: email_address_passcode_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(page(EmailAddressPasscode.form(), request.registration.primaryContactDetails.email))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      EmailAddressPasscode.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddressPasscode]) =>
            Future.successful(BadRequest(page(formWithErrors, None))),
          emailAddressPasscode =>
            FormAction.bindFromRequest match {
              case ContinueAction =>
                request.registration.primaryContactDetails.email match {
                  case Some(email) => continue(emailAddressPasscode.value, email)
                  case None        => throw RegistrationException("Failed to get email from the cache")
                }

              case _ =>
                Future(Redirect(commonRoutes.TaskListController.displayPage()))
            }
        )
    }

  private def continue(passcode: String, email: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Result] =
    verifyEmailPasscode(passcode,
                        email,
                        request.registration.primaryContactDetails.journeyId.getOrElse("")
    ).flatMap {
      case Right(COMPLETE) =>
        addVerifiedEmail(email) map {
          case Right(_) =>
            Redirect(routes.ContactDetailsEmailAddressPasscodeConfirmationController.displayPage())
          case Left(error) => throw error
        }
      case Right(INCORRECT_PASSCODE) =>
        Future.successful(
          BadRequest(
            page(EmailAddressPasscode.form().withError("incorrectPasscode", "Incorrect Passcode"),
                 None
            )
          )
        )
      case Right(TOO_MANY_ATTEMPTS) =>
        Future.successful(
          Redirect(
            routes.ContactDetailsTooManyAttemptsPasscodeController.displayPage()
          ).withNewSession
        )
      case Right(_) =>
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

  private def addVerifiedEmail(email: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(metaData = registration.metaData.add(Seq(EmailStatus(email, true, false))))
    }

  private def verifyEmailPasscode(passcode: String, email: String, journeyId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, JourneyStatus]] =
    emailVerificationConnector.verifyPasscode(
      journeyId = journeyId,
      VerifyPasscodeRequest(passcode = passcode, email = email)
    )

}
