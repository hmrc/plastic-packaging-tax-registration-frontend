/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.data.Form
import play.api.mvc._
import controllers.actions.JourneyAction
import controllers.amendment.{AmendmentController, routes => amendmentRoutes}
import controllers.{AddressLookupIntegration, EmailVerificationActions}
import forms.contact.{EmailAddress, EmailAddressPasscode, JobTitle, PhoneNumber}
import forms.group.MemberName
import models.genericregistration.Partner
import models.registration.{AmendRegistrationUpdateService, Registration}
import models.request.JourneyRequest
import services.{AddressCaptureConfig, AddressCaptureService, AmendRegistrationService, EmailVerificationService}
import views.html.contact.{email_address_passcode_confirmation_page, email_address_passcode_page, too_many_attempts_passcode_page}
import views.html.partner.{partner_email_address_page, partner_job_title_page, partner_member_name_page, partner_phone_number_page}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendPartnerContactDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  journeyAction: JourneyAction,
  amendRegistrationService: AmendRegistrationService,
  contactNamePage: partner_member_name_page,
  contactEmailPage: partner_email_address_page,
  val emailPasscodePage: email_address_passcode_page,
  val emailCorrectPasscodePage: email_address_passcode_confirmation_page,
  val emailIncorrectPasscodeTooManyAttemptsPage: too_many_attempts_passcode_page,
  val registrationUpdater: AmendRegistrationUpdateService,
  val emailVerificationService: EmailVerificationService,
  contactPhoneNumberPage: partner_phone_number_page,
  jobTitlePage: partner_job_title_page,
  addressCaptureService: AddressCaptureService
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendRegistrationService)
    with AddressLookupIntegration
    with EmailVerificationActions {

  def contactName(partnerId: String): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      val partner = getPartner(partnerId)
      val form = MemberName.form().fill(
        MemberName(
          firstName = partner.contactDetails.flatMap(_.firstName).getOrElse(
            throw new IllegalStateException("Partner first name absent")
          ),
          lastName = partner.contactDetails.flatMap(_.lastName).getOrElse(
            throw new IllegalStateException("Partner last name absent")
          )
        )
      )

      Ok(buildContactNamePage(form, partner, isNominated(partnerId)))
    }

  def updateContactName(partnerId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      MemberName.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[MemberName]) =>
            Future.successful(
              BadRequest(buildContactNamePage(formWithErrors, getPartner(partnerId), isNominated(partnerId)))
            ),
          partnerName =>
            updateRegistration(
              { registration: Registration =>
                registration.withUpdatedPartner(
                  partnerId,
                  partner =>
                    partner.copy(contactDetails =
                      partner.contactDetails.map(
                        _.copy(
                          firstName = Some(partnerName.firstName),
                          lastName = Some(partnerName.lastName)
                        )
                      )
                    )
                )
              },
              successfulRedirect(partnerId)
            )
        )
    }

  private def buildContactNamePage(form: Form[MemberName], partner: Partner, isNominated: Boolean)(implicit
    request: Request[_]
  ) =
    contactNamePage(
      form = form,
      partnershipName = partner.name,
      isNominated,
      updateCall = routes.AmendPartnerContactDetailsController.updateContactName(partner.id)
    )

  def emailAddress(partnerId: String): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      val form = EmailAddress.form().fill(
        EmailAddress(
          getPartner(partnerId).contactDetails.flatMap(_.emailAddress).getOrElse(
            throw new IllegalStateException("Partner email address absent")
          )
        )
      )
      Ok(buildContactEmailPage(form, getPartner(partnerId), isNominated(partnerId)))
    }

  def updateEmailAddress(partnerId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      val partner = getPartner(partnerId)
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(
              BadRequest(buildContactEmailPage(formWithErrors, getPartner(partnerId), isNominated(partnerId)))
            ),
          emailAddress =>
            doesPartnerEmailRequireVerification(partner, emailAddress).flatMap { isEmailVerificationRequired =>
              if (!isEmailVerificationRequired)
                updateRegistration(
                  { registration: Registration =>
                    registration.withUpdatedPartner(
                      partnerId,
                      partner => applyEmailAddressTo(partner, emailAddress.value)
                    )
                  },
                  successfulRedirect(partnerId)
                )
              else
                promptForEmailVerificationCode(
                  request,
                  emailAddress,
                  routes.AmendPartnerContactDetailsController.emailAddress(partner.id),
                  routes.AmendPartnerContactDetailsController.confirmEmailCode(partner.id)
                )
            }
        )
    }

  def confirmEmailCode(partnerId: String): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      val partner = getPartner(partnerId)
      Ok(
        renderEnterEmailVerificationCodePage(
          EmailAddressPasscode.form(),
          getProspectiveEmail(),
          routes.AmendPartnerContactDetailsController.emailAddress(partner.id),
          routes.AmendPartnerContactDetailsController.checkEmailVerificationCode(partnerId)
        )
      )
    }

  def checkEmailVerificationCode(partnerId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      val partner = getPartner(partnerId)
      def emailVerificationTooManyAttemptsCall =
        routes.AmendPartnerContactDetailsController.emailVerificationTooManyAttempts()
      processVerificationCodeSubmission(
        routes.AmendPartnerContactDetailsController.emailAddress(partner.id),
        routes.AmendPartnerContactDetailsController.checkEmailVerificationCode(partnerId),
        routes.AmendPartnerContactDetailsController.emailVerified(partnerId),
        emailVerificationTooManyAttemptsCall
      )
    }

  def emailVerified(partnerId: String): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      val partner = getPartner(partnerId)
      showEmailVerifiedPage(
        routes.AmendPartnerContactDetailsController.confirmEmailCode(partner.id),
        routes.AmendPartnerContactDetailsController.confirmEmailUpdate(partnerId)
      )
    }

  def emailVerificationTooManyAttempts(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      showTooManyAttemptsPage
    }

  def confirmEmailUpdate(partnerId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      val prospectiveEmail = getProspectiveEmail()
      isEmailVerified(prospectiveEmail).flatMap { isVerified =>
        if (isVerified)
          updateRegistration(
            { registration: Registration =>
              registration.withUpdatedPartner(partnerId, partner => applyEmailAddressTo(partner, prospectiveEmail))
            },
            successfulRedirect(partnerId)
          )
        else
          Future.successful(Redirect(routes.AmendPartnerContactDetailsController.emailAddress(partnerId)))
      }
    }

  private def buildContactEmailPage(form: Form[EmailAddress], partner: Partner, isNominated: Boolean)(implicit
    request: JourneyRequest[_]
  ) =
    contactEmailPage(
      form = form,
      updateCall = routes.AmendPartnerContactDetailsController.updateEmailAddress(partner.id),
      contactName = partner.name,
      isNominated
    )

  def phoneNumber(partnerId: String): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      val form =
        PhoneNumber.form().fill(
          PhoneNumber(
            getPartner(partnerId).contactDetails.flatMap(_.phoneNumber).getOrElse(
              throw new IllegalStateException("Partner phone number absent")
            )
          )
        )
      Ok(buildContactPhoneNumberPage(form, getPartner(partnerId), isNominated(partnerId)))
    }

  def updatePhoneNumber(partnerId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      PhoneNumber.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[PhoneNumber]) =>
            Future.successful(
              BadRequest(buildContactPhoneNumberPage(formWithErrors, getPartner(partnerId), isNominated(partnerId)))
            ),
          phoneNumber =>
            updateRegistration(
              { registration: Registration =>
                registration.withUpdatedPartner(
                  partnerId,
                  partner =>
                    partner.copy(contactDetails =
                      partner.contactDetails.map(_.copy(phoneNumber = Some(phoneNumber.value)))
                    )
                )
              },
              successfulRedirect(partnerId)
            )
        )
    }

  private def buildContactPhoneNumberPage(form: Form[PhoneNumber], partner: Partner, isNominated: Boolean)(implicit
    request: JourneyRequest[_]
  ) =
    contactPhoneNumberPage(
      form = form,
      updateCall = routes.AmendPartnerContactDetailsController.updatePhoneNumber(partner.id),
      contactName = partner.name,
      isNominated
    )

  def address(partnerId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      addressCaptureService.initAddressCapture(
        AddressCaptureConfig(
          backLink = routes.PartnerContactDetailsCheckAnswersController.displayPage(partnerId).url,
          successLink = routes.AmendPartnerContactDetailsController.updateAddress(partnerId).url,
          alfHeadingsPrefix = "addressLookup.partner",
          entityName = Some(getPartner(partnerId).name),
          pptHeadingKey = "addressCapture.contact.heading",
          pptHintKey = None,
          forceUkAddress = false
        )
      )(request.authenticatedRequest).map(redirect => Redirect(redirect))
    }

  def updateAddress(partnerId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      addressCaptureService.getCapturedAddress()(request.authenticatedRequest).flatMap { capturedAddress =>
        updateRegistration(
          registration =>
            registration.withUpdatedPartner(
              partnerId,
              partner => partner.copy(contactDetails = partner.contactDetails.map(_.copy(address = capturedAddress)))
            ),
          successfulRedirect(partnerId)
        )
      }
    }

  def jobTitle(partnerId: String): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      val partner = getPartner(partnerId)
      val form = JobTitle.form().fill(
        JobTitle(value =
          partner.contactDetails.flatMap(_.jobTitle).getOrElse(
            throw new IllegalStateException("Nominated Partner job title absent")
          )
        )
      )

      Ok(buildJobTitlePage(form, partner, isNominated(partnerId)))
    }

  def updateJobTitle(partnerId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      JobTitle.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[JobTitle]) =>
            Future.successful(
              BadRequest(buildJobTitlePage(formWithErrors, getPartner(partnerId), isNominated(partnerId)))
            ),
          jobTitle =>
            updateRegistration(
              { registration: Registration =>
                registration.withUpdatedPartner(
                  partnerId,
                  partner =>
                    partner.copy(contactDetails =
                      partner.contactDetails.map(
                        _.copy(jobTitle = Some(jobTitle.value))
                      )
                    )
                )
              },
              successfulRedirect(partnerId)
            )
        )
    }

  private def buildJobTitlePage(form: Form[JobTitle], partner: Partner, isNominated: Boolean)(implicit
    request: JourneyRequest[_]
  ) =
    jobTitlePage(
      form = form,
      contactName = partner.name,
      updateCall = routes.AmendPartnerContactDetailsController.updateJobTitle(partner.id)
    )

  private def getPartner(partnerId: String)(implicit request: JourneyRequest[_]): Partner =
    request.registration.findPartner(partnerId).getOrElse(throw new IllegalStateException("Partner not found"))

  private def isNominated(partnerId: String)(implicit request: JourneyRequest[_]) =
    request.registration.isNominatedPartner(Some(partnerId))

  private def successfulRedirect(partnerId: String)(implicit request: JourneyRequest[_]) =
    if (isNominated(partnerId)) amendmentRoutes.AmendRegistrationController.displayPage()
    else routes.PartnerContactDetailsCheckAnswersController.displayPage(partnerId)

  private def applyEmailAddressTo(partner: Partner, emailAddress: String): Partner =
    partner.copy(contactDetails = partner.contactDetails.map(_.copy(emailAddress = Some(emailAddress))))

}
