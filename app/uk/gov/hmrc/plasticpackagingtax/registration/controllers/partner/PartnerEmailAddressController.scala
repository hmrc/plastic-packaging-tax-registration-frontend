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

import play.api.data.Form
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddressPasscode
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.NewRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.services.EmailVerificationService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.{
  email_address_passcode_confirmation_page,
  email_address_passcode_page
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
  email_address_passcode_confirmation_page: email_address_passcode_confirmation_page,
  val registrationUpdateService: NewRegistrationUpdateService,
  val emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext)
    extends PartnerEmailAddressControllerBase(authenticate = authenticate,
                                              journeyAction = journeyAction,
                                              mcc = mcc,
                                              page = page,
                                              registrationUpdater = registrationUpdateService
    ) {

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
      request.registration.inflightPartner.map { partner =>
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
      request.registration.inflightPartner.map { partner =>
        EmailAddressPasscode.form()
          .bindFromRequest()
          .fold(
            (formWithErrors: Form[EmailAddressPasscode]) =>
              Future.successful(
                BadRequest(
                  renderEnterEmailVerificationCodePage(formWithErrors,
                                                       getProspectiveEmail(),
                                                       routes.PartnerEmailAddressController.displayNewPartner(),
                                                       routes.PartnerEmailAddressController.checkNewPartnerEmailVerificationCode()
                  )
                )
              ),
            verificationCode =>
              handleEmailVerificationCodeSubmission(verificationCode.value,
                                                    routes.PartnerEmailAddressController.emailVerified(),
                                                    routes.PartnerEmailAddressController.emailVerificationTooManyAttempts(),
                                                    routes.PartnerEmailAddressController.displayNewPartner(),
                                                    routes.PartnerEmailAddressController.checkNewPartnerEmailVerificationCode()
              )
          )

      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def emailVerified(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok("TODO")
    }

  def emailVerificationTooManyAttempts(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok("TODO")
    }

  def confirmExistingPartnerEmailCode(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      getPartner(Some(partnerId)).map { partner =>
        Ok(
          emailPasscodePage(EmailAddressPasscode.form(),
                            Some(getProspectiveEmail()),
                            routes.PartnerEmailAddressController.displayExistingPartner(partnerId),
                            routes.PartnerEmailAddressController.checkExistingPartnerEmailVerificationCode(
                              partnerId
                            )
          )
        )
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

  def checkExistingPartnerEmailVerificationCode(partnerId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      getPartner(Some(partnerId)).map { partner =>
        Ok("TODO")
      }.getOrElse(throw new IllegalStateException("Expected partner missing"))
    }

}
