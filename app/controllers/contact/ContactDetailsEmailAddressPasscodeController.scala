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

package controllers.contact

import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.{
  NotEnrolledAuthAction,
  FormAction,
  Continue => ContinueAction
}
import controllers.{routes => commonRoutes}
import forms.contact.EmailAddressPasscode
import models.emailverification.EmailVerificationJourneyStatus.{
  COMPLETE,
  INCORRECT_PASSCODE,
  TOO_MANY_ATTEMPTS
}
import models.emailverification.{
  EmailStatus,
  EmailVerificationJourneyStatus
}
import models.registration.{Cacheable, Registration}
import models.request.{JourneyAction, JourneyRequest}
import services.EmailVerificationService
import views.html.contact.email_address_passcode_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactDetailsEmailAddressPasscodeController @Inject() (
                                                               authenticate: NotEnrolledAuthAction,
                                                               journeyAction: JourneyAction,
                                                               mcc: MessagesControllerComponents,
                                                               emailVerificationService: EmailVerificationService,
                                                               override val registrationConnector: RegistrationConnector,
                                                               page: email_address_passcode_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(
        buildEmailPasscodePage(EmailAddressPasscode.form(),
                               request.registration.primaryContactDetails.email
        )
      )
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      EmailAddressPasscode.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddressPasscode]) =>
            Future.successful(BadRequest(buildEmailPasscodePage(formWithErrors, None))),
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
  ): Future[Result] = {
    val eventualValue = verifyEmailPasscode(
      passcode,
      email,
      request.registration.primaryContactDetails.journeyId.getOrElse("")
    )

    eventualValue.flatMap {
      case COMPLETE =>
        addVerifiedEmail(email) map {
          case Right(_) =>
            Redirect(routes.ContactDetailsEmailAddressPasscodeConfirmationController.displayPage())
          case Left(error) => throw error
        }
      case INCORRECT_PASSCODE =>
        Future.successful(
          BadRequest(
            buildEmailPasscodePage(EmailAddressPasscode.form().withError(
                                     "incorrectPasscode",
                                     "primaryContactDetails.emailAddress.passcode.incorrect"
                                   ),
                                   None
            )
          )
        )
      case TOO_MANY_ATTEMPTS =>
        Future.successful(
          Redirect(
            routes.ContactDetailsTooManyAttemptsPasscodeController.displayPage()
          ).withNewSession
        )
      case _ =>
        Future.successful(
          BadRequest(
            buildEmailPasscodePage(
              EmailAddressPasscode.form().withError("journeyNotFound",
                                                    "Passcode for email address is not found"
              ),
              None
            )
          )
        )
    }
  }

  private def buildEmailPasscodePage(form: Form[EmailAddressPasscode], email: Option[String])(
    implicit request: JourneyRequest[AnyContent]
  ) =
    page(form,
         email,
         routes.ContactDetailsEmailAddressPasscodeController.submit(),
         Some(sectionName)
    )

  private def addVerifiedEmail(email: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(metaData = registration.metaData.add(Seq(EmailStatus(email, true, false))))
    }

  private def verifyEmailPasscode(passcode: String, email: String, journeyId: String)(implicit
    hc: HeaderCarrier
  ): Future[EmailVerificationJourneyStatus.Value] =
    emailVerificationService.checkVerificationCode(passcode, email, journeyId)

  private def sectionName()(implicit request: JourneyRequest[AnyContent], messages: Messages): String = {
    if(request.registration.isGroup)
      messages("primaryContactDetails.group.sectionHeader")
    else messages("primaryContactDetails.sectionHeader")
  }

}
