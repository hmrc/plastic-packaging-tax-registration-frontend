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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddressPasscode
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  NewRegistrationUpdateService,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.services.EmailVerificationService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.{
  email_address_passcode_confirmation_page,
  email_address_passcode_page,
  too_many_attempts_passcode_page
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_email_address_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerEmailAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: partner_email_address_page,
  val emailPasscodePage: email_address_passcode_page,
  val emailCorrectPasscodePage: email_address_passcode_confirmation_page,
  val emailIncorrectPasscodeTooManyAttemptsPage: too_many_attempts_passcode_page,
  val registrationUpdateService: NewRegistrationUpdateService,
  val emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext)
    extends PartnerEmailAddressControllerBase(authenticate = authenticate,
                                              journeyAction = journeyAction,
                                              mcc = mcc,
                                              page = page,
                                              registrationUpdater = registrationUpdateService
    ) {

  private def emailVerificationTooManyAttemptsCall =
    routes.PartnerEmailAddressController.emailVerificationTooManyAttempts()

  def displayNewPartner(): Action[AnyContent] =
    doDisplay(None,
              partnerRoutes.PartnerContactNameController.displayNewPartner(),
              partnerRoutes.PartnerEmailAddressController.submitNewPartner()
    )

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplay(Some(partnerId),
              partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
              partnerRoutes.PartnerEmailAddressController.submitExistingPartner(partnerId)
    )

  def submitNewPartner(): Action[AnyContent] =
    doSubmit(None,
             partnerRoutes.PartnerContactNameController.displayNewPartner(),
             partnerRoutes.PartnerEmailAddressController.submitNewPartner(),
             partnerRoutes.PartnerPhoneNumberController.displayNewPartner(),
             commonRoutes.TaskListController.displayPage(),
             partnerRoutes.PartnerContactNameController.displayNewPartner(),
             partnerRoutes.PartnerEmailAddressController.confirmNewPartnerEmailCode()
    )

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    doSubmit(Some(partnerId),
             partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
             partnerRoutes.PartnerEmailAddressController.submitExistingPartner(partnerId),
             routes.PartnerPhoneNumberController.displayExistingPartner(partnerId),
             partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partnerId),
             partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partnerId),
             partnerRoutes.PartnerEmailAddressController.confirmExistingPartnerEmailCode(partnerId)
    )

  def confirmNewPartnerEmailCode(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.inflightPartner.map { _ =>
        Ok(
          renderEnterEmailVerificationCodePage(EmailAddressPasscode.form(),
                                               getProspectiveEmail(),
                                               routes.PartnerEmailAddressController.displayNewPartner(),
                                               routes.PartnerEmailAddressController.checkNewPartnerEmailVerificationCode()
          )
        )

      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def checkNewPartnerEmailVerificationCode(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.inflightPartner.map { _ =>
        processVerificationCodeSubmission(routes.PartnerEmailAddressController.displayNewPartner(),
                                          routes.PartnerEmailAddressController.checkNewPartnerEmailVerificationCode(),
                                          routes.PartnerEmailAddressController.emailVerifiedNewPartner(),
                                          emailVerificationTooManyAttemptsCall
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def emailVerifiedNewPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.inflightPartner.map { _ =>
        showEmailVerifiedPage(routes.PartnerEmailAddressController.confirmNewPartnerEmailCode(),
                              routes.PartnerEmailAddressController.confirmEmailUpdateNewPartner()
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def confirmEmailUpdateNewPartner(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
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
    (authenticate andThen journeyAction) { implicit request =>
      showTooManyAttemptsPage
    }

  def confirmExistingPartnerEmailCode(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
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
    (authenticate andThen journeyAction).async { implicit request =>
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
    (authenticate andThen journeyAction) { implicit request =>
      getPartner(Some(partnerId)).map { _ =>
        showEmailVerifiedPage(
          routes.PartnerEmailAddressController.confirmExistingPartnerEmailCode(partnerId),
          routes.PartnerEmailAddressController.confirmEmailUpdateExistingPartner(partnerId)
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def emailVerifiedExistingPartner(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
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
