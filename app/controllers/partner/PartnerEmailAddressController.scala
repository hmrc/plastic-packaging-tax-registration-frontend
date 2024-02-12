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

package controllers.partner

import controllers.actions.JourneyAction
import controllers.partner.{routes => partnerRoutes}
import forms.contact.EmailAddressPasscode
import models.genericregistration.Partner
import models.registration.{NewRegistrationUpdateService, Registration}
import play.api.mvc._
import services.EmailVerificationService
import views.html.contact.{email_address_passcode_confirmation_page, email_address_passcode_page, too_many_attempts_passcode_page}
import views.html.partner.partner_email_address_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerEmailAddressController @Inject() (
                                                journeyAction: JourneyAction,
                                                mcc: MessagesControllerComponents,
                                                page: partner_email_address_page,
                                                val emailPasscodePage: email_address_passcode_page,
                                                val emailCorrectPasscodePage: email_address_passcode_confirmation_page,
                                                val emailIncorrectPasscodeTooManyAttemptsPage: too_many_attempts_passcode_page,
                                                val registrationUpdateService: NewRegistrationUpdateService,
                                                val emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext)
    extends PartnerEmailAddressControllerBase(
                                              journeyAction = journeyAction.register,
                                              mcc = mcc,
                                              page = page,
                                              registrationUpdater = registrationUpdateService
    ) {

  private def emailVerificationTooManyAttemptsCall =
    routes.PartnerEmailAddressController.emailVerificationTooManyAttempts()

  def displayNewPartner(): Action[AnyContent] =
    doDisplay(None,
              partnerRoutes.PartnerContactNameController.displayNewPartner,
              partnerRoutes.PartnerEmailAddressController.submitNewPartner()
    )

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplay(Some(partnerId),
              partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
              partnerRoutes.PartnerEmailAddressController.submitExistingPartner(partnerId)
    )

  def submitNewPartner(): Action[AnyContent] =
    doSubmit(None,
             partnerRoutes.PartnerContactNameController.displayNewPartner,
             partnerRoutes.PartnerEmailAddressController.submitNewPartner(),
             partnerRoutes.PartnerPhoneNumberController.displayNewPartner(),
             partnerRoutes.PartnerContactNameController.displayNewPartner,
             partnerRoutes.PartnerEmailAddressController.confirmNewPartnerEmailCode()
    )

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    doSubmit(Some(partnerId),
             partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
             partnerRoutes.PartnerEmailAddressController.submitExistingPartner(partnerId),
             routes.PartnerPhoneNumberController.displayExistingPartner(partnerId),
             partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partnerId),
             partnerRoutes.PartnerEmailAddressController.confirmExistingPartnerEmailCode(partnerId)
    )

  def confirmNewPartnerEmailCode(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.inflightPartner.map { _ =>
        Ok(
          renderEnterEmailVerificationCodePage(EmailAddressPasscode.form(),
                                               getProspectiveEmail(),
                                               routes.PartnerEmailAddressController.displayNewPartner(),
                                               routes.PartnerEmailAddressController.checkNewPartnerEmailVerificationCode(),
            Some("partnership.job-title-page.section-header")
          )
        )

      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def checkNewPartnerEmailVerificationCode(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      request.registration.inflightPartner.map { _ =>
        processVerificationCodeSubmission(routes.PartnerEmailAddressController.displayNewPartner(),
                                          routes.PartnerEmailAddressController.checkNewPartnerEmailVerificationCode(),
                                          routes.PartnerEmailAddressController.emailVerifiedNewPartner(),
                                          emailVerificationTooManyAttemptsCall
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def emailVerifiedNewPartner(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.inflightPartner.map { _ =>
        showEmailVerifiedPage(
          routes.PartnerEmailAddressController.confirmNewPartnerEmailCode(),
          routes.PartnerEmailAddressController.confirmEmailUpdateNewPartner(),
          Some("partnership.nominated-email-verified.section-heading")
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def confirmEmailUpdateNewPartner(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      request.registration.inflightPartner.map { _ =>
        val prospectiveEmail = getProspectiveEmail()
        isEmailVerified(prospectiveEmail).flatMap { isVerified =>
          if (isVerified)
            registrationUpdater.updateRegistration(
              updatePartnersEmail(None, prospectiveEmail)
            ).map { _ =>
              Redirect(routes.PartnerPhoneNumberController.displayNewPartner())
            }
          else
            Future.successful(Redirect(routes.PartnerEmailAddressController.displayNewPartner()))
        }
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def emailVerificationTooManyAttempts(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      showTooManyAttemptsPage
    }

  def confirmExistingPartnerEmailCode(partnerId: String): Action[AnyContent] =
    journeyAction.register { implicit request =>
      getPartner(Some(partnerId)).map { _ =>
        Ok(
          renderEnterEmailVerificationCodePage(EmailAddressPasscode.form(),
                                               getProspectiveEmail(),
                                               routes.PartnerEmailAddressController.displayExistingPartner(
                                                 partnerId
                                               ),
                                               routes.PartnerEmailAddressController.checkExistingPartnerEmailVerificationCode(
                                                 partnerId
                                               )
          )
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def checkExistingPartnerEmailVerificationCode(partnerId: String): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      getPartner(Some(partnerId)).map { _ =>
        processVerificationCodeSubmission(
          routes.PartnerEmailAddressController.displayExistingPartner(partnerId),
          routes.PartnerEmailAddressController.checkExistingPartnerEmailVerificationCode(partnerId),
          routes.PartnerEmailAddressController.emailVerifiedExistingPartner(partnerId),
          emailVerificationTooManyAttemptsCall
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def confirmEmailUpdateExistingPartner(partnerId: String): Action[AnyContent] =
    journeyAction.register { implicit request =>
      getPartner(Some(partnerId)).map { _ =>
        showEmailVerifiedPage(
          routes.PartnerEmailAddressController.confirmExistingPartnerEmailCode(partnerId),
          routes.PartnerEmailAddressController.confirmEmailUpdateExistingPartner(partnerId)
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def emailVerifiedExistingPartner(partnerId: String): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      getPartner(Some(partnerId)).map { partner =>
        registrationUpdater.updateRegistration(
          updatePartnersEmail(Some(partner), getProspectiveEmail())
        ).map { _ =>
          Redirect(routes.PartnerPhoneNumberController.displayExistingPartner(partnerId))
        }
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  private def updatePartnersEmail(
    partner: Option[Partner],
    updatedEmail: String
  ): Registration => Registration = {
    registration: Registration =>
      updateRegistrationWithPartnerEmail(registration, partner.map(_.id), updatedEmail)
  }

}
