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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, Result}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.{
  EmailAddress,
  EmailAddressPasscode
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus.{
  COMPLETE,
  INCORRECT_PASSCODE,
  TOO_MANY_ATTEMPTS
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Registration,
  RegistrationUpdater
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.plasticpackagingtax.registration.services.EmailVerificationService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.{
  email_address_passcode_confirmation_page,
  email_address_passcode_page,
  too_many_attempts_passcode_page
}

import scala.concurrent.{ExecutionContext, Future}

trait EmailVerificationActions {

  def emailVerificationService: EmailVerificationService
  def registrationUpdater: RegistrationUpdater
  def emailPasscodePage: email_address_passcode_page
  def emailCorrectPasscodePage: email_address_passcode_confirmation_page
  def emailIncorrectPasscodeTooManyAttemptsPage: too_many_attempts_passcode_page

  def isEmailVerificationRequired(email: String, isEmailChanged: String => Boolean)(implicit
    request: JourneyRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    if (isEmailChanged(email))
      emailVerificationService.isEmailVerified(email, request.user.credId).map(!_)
    else
      Future.successful(false)

  def promptForEmailVerificationCode(
    request: JourneyRequest[AnyContent],
    email: EmailAddress,
    continueUrl: String,
    enterVerificationCodeCall: Call
  )(implicit
    journeyRequest: JourneyRequest[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] =
    emailVerificationService.sendVerificationCode(email.value,
                                                  request.user.credId,
                                                  continueUrl
    ).map { emailVerificationJourneyId =>
      persistProspectiveEmailAddress(email, emailVerificationJourneyId)
      Redirect(enterVerificationCodeCall)
    }

  def handleEmailVerificationCodeSubmission(
    verificationCode: String,
    successCall: Call,
    tooManyAttemptsCall: Call,
    backCall: Call,
    submitCall: Call
  )(implicit
    journeyRequest: JourneyRequest[AnyContent],
    messages: Messages,
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] =
    checkVerificationCode(verificationCode).map {
      case COMPLETE =>
        Redirect(successCall)
      case INCORRECT_PASSCODE =>
        BadRequest(
          renderEnterEmailVerificationCodePage(
            EmailAddressPasscode.form().withError("incorrectPasscode", "Incorrect Passcode"),
            getProspectiveEmail(),
            backCall,
            submitCall
          )
        )
      case TOO_MANY_ATTEMPTS =>
        Redirect(tooManyAttemptsCall)
      case _ =>
        BadRequest(
          renderEnterEmailVerificationCodePage(
            EmailAddressPasscode.form().withError("journeyNotFound",
                                                  "Passcode for email address is not found"
            ),
            getProspectiveEmail(),
            backCall,
            submitCall
          )
        )
    }

  def showEmailVerifiedPage(backCall: Call, submitCall: Call)(implicit
    request: JourneyRequest[AnyContent],
    messages: Messages
  ): Result =
    Ok(emailCorrectPasscodePage(backCall, submitCall))

  def showTooManyAttemptsPage()(implicit
    request: JourneyRequest[AnyContent],
    messages: Messages
  ): Result =
    Ok(emailIncorrectPasscodeTooManyAttemptsPage())

  private def checkVerificationCode(verificationCode: String)(implicit
    req: JourneyRequest[AnyContent],
    hc: HeaderCarrier
  ): Future[EmailVerificationJourneyStatus.Value] =
    emailVerificationService.checkVerificationCode(verificationCode,
                                                   getProspectiveEmail(),
                                                   getEmailVerificationJourneyId()
    )

  protected def getProspectiveEmail()(implicit req: JourneyRequest[AnyContent]): String =
    req.registration.primaryContactDetails.prospectiveEmail.getOrElse(
      throw new IllegalStateException("Prospective email expected in registration")
    )

  private def persistProspectiveEmailAddress(
    email: EmailAddress,
    emailVerificationJourneyId: String
  )(implicit journeyRequest: JourneyRequest[AnyContent], hc: HeaderCarrier): Future[Registration] =
    // By updating and persisting the entire registration; all the way back to ETMP in the case of an amendment
    registrationUpdater.updateRegistration(
      setProspectiveEmailOnRegistration(email.value, emailVerificationJourneyId)
    )

  private def setProspectiveEmailOnRegistration(
    prospectiveEmail: String,
    emailVerificationJourneyId: String
  ) = {
    registration: Registration =>
      registration.copy(primaryContactDetails =
        registration.primaryContactDetails.copy(journeyId = Some(emailVerificationJourneyId),
                                                prospectiveEmail = Some(prospectiveEmail)
        )
      )
  }

  private def getEmailVerificationJourneyId()(implicit req: JourneyRequest[AnyContent]) =
    req.registration.primaryContactDetails.journeyId.getOrElse(
      throw new IllegalStateException("Journey id expected in registration")
    )

  protected def renderEnterEmailVerificationCodePage(
    form: Form[EmailAddressPasscode],
    prospectiveEmailAddress: String,
    backCall: Call,
    submitCall: Call
  )(implicit request: JourneyRequest[AnyContent], messages: Messages): HtmlFormat.Appendable =
    emailPasscodePage(form, Some(prospectiveEmailAddress), backCall, submitCall)

}
