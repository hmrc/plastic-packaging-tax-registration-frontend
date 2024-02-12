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

package controllers.amendment.partner

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.JourneyAction
import controllers.partner.PartnerEmailAddressControllerBase
import forms.contact.EmailAddressPasscode
import models.registration.{AmendRegistrationUpdateService, Registration}
import services.EmailVerificationService
import views.html.contact.{email_address_passcode_confirmation_page, email_address_passcode_page, too_many_attempts_passcode_page}
import views.html.partner.partner_email_address_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddPartnerContactDetailsEmailAddressController @Inject() (
                                                                 journeyAction: JourneyAction,
                                                                 mcc: MessagesControllerComponents,
                                                                 page: partner_email_address_page,
                                                                 val emailPasscodePage: email_address_passcode_page,
                                                                 val emailCorrectPasscodePage: email_address_passcode_confirmation_page,
                                                                 val emailIncorrectPasscodeTooManyAttemptsPage: too_many_attempts_passcode_page,
                                                                 registrationUpdateService: AmendRegistrationUpdateService,
                                                                 val emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext)
    extends PartnerEmailAddressControllerBase(journeyAction = journeyAction.amend,
                                              mcc = mcc,
                                              page = page,
                                              registrationUpdater = registrationUpdateService
    ) {

  def displayPage(): Action[AnyContent] =
    doDisplay(None,
              routes.AddPartnerContactDetailsNameController.displayPage,
              routes.AddPartnerContactDetailsEmailAddressController.submit()
    )

  def submit(): Action[AnyContent] =
    doSubmit(None,
             routes.AddPartnerContactDetailsNameController.displayPage,
             routes.AddPartnerContactDetailsEmailAddressController.submit(),
             routes.AddPartnerContactDetailsTelephoneNumberController.displayPage(),
             routes.AddPartnerContactDetailsEmailAddressController.displayPage(),
             routes.AddPartnerContactDetailsEmailAddressController.confirmEmailCode()
    )

  def confirmEmailCode(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      Ok(
        renderEnterEmailVerificationCodePage(EmailAddressPasscode.form(),
                                             getProspectiveEmail(),
                                             routes.AddPartnerContactDetailsEmailAddressController.displayPage(),
                                             routes.AddPartnerContactDetailsEmailAddressController.checkEmailVerificationCode()
        )
      )
    }

  def checkEmailVerificationCode(): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      processVerificationCodeSubmission(
        routes.AddPartnerContactDetailsEmailAddressController.displayPage(),
        routes.AddPartnerContactDetailsEmailAddressController.checkEmailVerificationCode(),
        routes.AddPartnerContactDetailsEmailAddressController.emailVerified(),
        routes.AddPartnerContactDetailsEmailAddressController.emailVerificationTooManyAttempts()
      )
    }

  def emailVerified(): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      val prospectiveEmail = getProspectiveEmail()
      isEmailVerified(prospectiveEmail).flatMap { isVerified =>
        if (isVerified)
          registrationUpdater.updateRegistration(updatePartnersEmail(prospectiveEmail)).map {
            _ =>
              Redirect(routes.AddPartnerContactDetailsTelephoneNumberController.displayPage())
          }
        else
          Future.successful(
            Redirect(routes.AddPartnerContactDetailsEmailAddressController.displayPage())
          )
      }
    }

  def confirmEmailUpdate(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      showEmailVerifiedPage(
        routes.AddPartnerContactDetailsEmailAddressController.confirmEmailCode(),
        routes.AddPartnerContactDetailsEmailAddressController.confirmEmailUpdate()
      )
    }

  def emailVerificationTooManyAttempts(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      showTooManyAttemptsPage
    }

  private def updatePartnersEmail(updatedEmail: String): Registration => Registration = {
    registration: Registration =>
      updateRegistrationWithPartnerEmail(registration, None, updatedEmail)
  }

}
