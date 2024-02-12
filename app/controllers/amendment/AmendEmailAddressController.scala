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

package controllers.amendment

import play.api.data.Form
import play.api.mvc._
import controllers.EmailVerificationActions
import controllers.actions.JourneyAction
import forms.contact._
import models.registration.{AmendRegistrationUpdateService, Registration}
import models.request.JourneyRequest
import services.{AmendRegistrationService, EmailVerificationService}
import views.html.contact._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendEmailAddressController @Inject() (
                                              mcc: MessagesControllerComponents,
                                              journeyAction: JourneyAction,
                                              amendRegistrationService: AmendRegistrationService,
                                              emailPage: email_address_page,
                                              val emailPasscodePage: email_address_passcode_page,
                                              val emailCorrectPasscodePage: email_address_passcode_confirmation_page,
                                              val emailIncorrectPasscodeTooManyAttemptsPage: too_many_attempts_passcode_page,
                                              val emailVerificationService: EmailVerificationService,
                                              val registrationUpdater: AmendRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendRegistrationService) with EmailVerificationActions {

  private def backCall   = routes.AmendEmailAddressController.email()
  private def submitCall = routes.AmendEmailAddressController.checkEmailVerificationCode()

  def email(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      request.registration.primaryContactDetails.email match {
        case Some(email) =>
          Ok(buildEmailPage(EmailAddress.form().fill(EmailAddress(email))))
        case _ =>
          Ok(buildEmailPage(EmailAddress.form()))
      }
    }

  def updateEmail(): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(BadRequest(buildEmailPage(formWithErrors))),
          email =>
            if (!isEmailChanged(email.value))
              // Not update required; we can exit straight to the next screen
              Future.successful(Redirect(routes.AmendRegistrationController.displayPage()))
            else
              isEmailVerificationRequired(email.value, isEmailChanged).flatMap {
                case false => updateRegistration(updateEmail(email.value))
                case true =>
                  promptForEmailVerificationCode(
                    request,
                    email,
                    routes.AmendRegistrationController.displayPage(),
                    routes.AmendEmailAddressController.emailVerificationCode()
                  )
              }
        )
    }

  private def isEmailChanged(newEmail: String)(implicit request: JourneyRequest[AnyContent]) =
    !request.registration.primaryContactDetails.email.contains(newEmail)

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
              routes.AmendEmailAddressController.updateEmail()
    )

  def emailVerificationCode(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      Ok(
        renderEnterEmailVerificationCodePage(EmailAddressPasscode.form(),
                                             getProspectiveEmail(),
                                             backCall,
                                             submitCall
        )
      )
    }

  def checkEmailVerificationCode(): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      processVerificationCodeSubmission(
        backCall,
        submitCall,
        routes.AmendEmailAddressController.emailVerified(),
        routes.AmendEmailAddressController.emailVerificationTooManyAttempts()
      )
    }

  def emailVerified(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      showEmailVerifiedPage(routes.AmendEmailAddressController.emailVerificationCode(),
                            routes.AmendEmailAddressController.confirmEmailUpdate()
      )
    }

  def confirmEmailUpdate(): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      val prospectiveEmail = getProspectiveEmail()
      isEmailVerified(prospectiveEmail).flatMap { isVerified =>
        if (isVerified)
          updateRegistration(updateEmail(prospectiveEmail))
        else
          Future.successful(Redirect(routes.AmendEmailAddressController.email()))
      }
    }

  def emailVerificationTooManyAttempts(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      showTooManyAttemptsPage
    }

}
