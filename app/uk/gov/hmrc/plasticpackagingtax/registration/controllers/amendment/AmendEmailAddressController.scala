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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact._
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus.{
  COMPLETE,
  INCORRECT_PASSCODE,
  TOO_MANY_ATTEMPTS
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.EmailVerificationService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendEmailAddressController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  mcc: MessagesControllerComponents,
  amendmentJourneyAction: AmendmentJourneyAction,
  emailPage: email_address_page,
  emailPasscodePage: email_address_passcode_page,
  emailCorrectPasscodePage: email_address_passcode_confirmation_page,
  emailIncorrectPasscodeTooManyAttemptsPage: too_many_attempts_passcode_page,
  emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def email(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      request.registration.primaryContactDetails.email match {
        case Some(email) =>
          Ok(buildEmailPage(EmailAddress.form().fill(EmailAddress(email))))
        case _ =>
          Ok(buildEmailPage(EmailAddress.form()))
      }
    }

  def updateEmail(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(BadRequest(buildEmailPage(formWithErrors))),
          email =>
            if (request.registration.primaryContactDetails.email.contains(email.value))
              Future.successful(Redirect(routes.AmendRegistrationController.displayPage()))
            else
              emailVerificationService.isEmailVerified(email.value).flatMap {
                case true => updateRegistration(updateEmail(email.value))
                case false =>
                  emailVerificationService.sendVerificationCode(email.value).map { journeyId =>
                    amendmentJourneyAction.updateLocalRegistration(
                      updateProspectiveEmail(journeyId, email.value)
                    )
                    Redirect(routes.AmendEmailAddressController.emailVerificationCode())
                  }
              }
        )
    }

  private def updateProspectiveEmail(
    journeyId: String,
    updatedEmail: String
  ): Registration => Registration = {
    registration: Registration =>
      registration.copy(primaryContactDetails =
        registration.primaryContactDetails.copy(journeyId = Some(journeyId),
                                                prospectiveEmail = Some(updatedEmail)
        )
      )
  }

  private def updateEmail(updatedEmail: String): Registration => Registration = {
    registration: Registration =>
      registration.copy(primaryContactDetails =
        registration.primaryContactDetails.copy(email = Some(updatedEmail))
      )
  }

  private def buildEmailPage(
    form: Form[EmailAddress]
  )(implicit request: JourneyRequest[AnyContent]) =
    emailPage(form,
              routes.AmendRegistrationController.displayPage(),
              routes.AmendEmailAddressController.updateEmail()
    )

  def emailVerificationCode(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      Ok(buildEmailVerificationCodePage(EmailAddressPasscode.form(), getProspectiveEmail()))
    }

  def checkEmailVerificationCode(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      EmailAddressPasscode.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddressPasscode]) =>
            Future.successful(
              BadRequest(buildEmailVerificationCodePage(formWithErrors, getProspectiveEmail()))
            ),
          verificationCode =>
            emailVerificationService.checkVerificationCode(verificationCode.value,
                                                           getProspectiveEmail(),
                                                           getJourneyId()
            ).flatMap {
              case COMPLETE =>
                Future.successful(Redirect(routes.AmendEmailAddressController.emailVerified()))
              case INCORRECT_PASSCODE =>
                Future.successful(
                  BadRequest(
                    buildEmailVerificationCodePage(
                      EmailAddressPasscode.form().withError("incorrectPasscode",
                                                            "Incorrect Passcode"
                      ),
                      getProspectiveEmail()
                    )
                  )
                )
              case TOO_MANY_ATTEMPTS =>
                Future.successful(
                  Redirect(routes.AmendEmailAddressController.emailVerificationTooManyAttempts())
                )
              case _ =>
                Future.successful(
                  BadRequest(
                    buildEmailVerificationCodePage(EmailAddressPasscode.form().withError(
                                                     "journeyNotFound",
                                                     "Passcode for email address is not found"
                                                   ),
                                                   getProspectiveEmail()
                    )
                  )
                )
            }
        )
    }

  private def getProspectiveEmail()(implicit req: JourneyRequest[AnyContent]) =
    req.registration.primaryContactDetails.prospectiveEmail.getOrElse(
      throw new IllegalStateException("Prospective email expected in registration")
    )

  private def getJourneyId()(implicit req: JourneyRequest[AnyContent]) =
    req.registration.primaryContactDetails.journeyId.getOrElse(
      throw new IllegalStateException("Journey id expected in registration")
    )

  private def buildEmailVerificationCodePage(form: Form[EmailAddressPasscode], email: String)(
    implicit request: JourneyRequest[AnyContent]
  ) =
    emailPasscodePage(form,
                      Some(email),
                      routes.AmendEmailAddressController.email(),
                      routes.AmendEmailAddressController.checkEmailVerificationCode()
    )

  def emailVerified(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      Ok(
        emailCorrectPasscodePage(routes.AmendEmailAddressController.emailVerificationCode(),
                                 routes.AmendEmailAddressController.confirmEmailUpdate()
        )
      )
    }

  def confirmEmailUpdate(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      updateRegistration(updateEmail(getProspectiveEmail()))
    }

  def emailVerificationTooManyAttempts(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      Ok(emailIncorrectPasscodeTooManyAttemptsPage())
    }

  private def updateRegistration(
    registrationAmendment: Registration => Registration
  )(implicit request: AuthenticatedRequest[Any], hc: HeaderCarrier) =
    amendmentJourneyAction.updateRegistration(registrationAmendment)
      .map(_ => Redirect(routes.AmendRegistrationController.displayPage()))
      .recover { case _ => Redirect(routes.AmendRegistrationController.registrationUpdateFailed()) }

}
