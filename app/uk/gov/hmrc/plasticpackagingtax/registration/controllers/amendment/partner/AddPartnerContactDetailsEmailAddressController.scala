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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.PartnerEmailAddressControllerBase
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddressPasscode
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.AmendRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.EmailVerificationService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.{
  email_address_passcode_confirmation_page,
  email_address_passcode_page
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_email_address_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerContactDetailsEmailAddressController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  journeyAction: AmendmentJourneyAction,
  mcc: MessagesControllerComponents,
  page: partner_email_address_page,
  email_address_passcode_page: email_address_passcode_page,
  email_address_passcode_confirmation_page: email_address_passcode_confirmation_page,
  registrationUpdateService: AmendRegistrationUpdateService,
  val emailVerificationService: EmailVerificationService
)(implicit ec: ExecutionContext)
    extends PartnerEmailAddressControllerBase(authenticate = authenticate,
                                              journeyAction = journeyAction,
                                              mcc = mcc,
                                              page = page,
                                              registrationUpdater = registrationUpdateService
    ) {

  def displayPage(): Action[AnyContent] =
    doDisplay(None,
              routes.AddPartnerContactDetailsNameController.displayPage(),
              routes.AddPartnerContactDetailsEmailAddressController.submit()
    )

  def submit(): Action[AnyContent] =
    doSubmit(None,
             routes.AddPartnerContactDetailsNameController.displayPage(),
             routes.AddPartnerContactDetailsEmailAddressController.submit(),
             routes.AddPartnerContactDetailsTelephoneNumberController.displayPage(),
             routes.PartnersListController.displayPage(),
             routes.AddPartnerContactDetailsEmailAddressController.displayPage(),
             routes.AddPartnerContactDetailsEmailAddressController.confirmEmailCode()
    )

  def confirmEmailCode(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(
        renderEnterEmailVerificationCodePage(EmailAddressPasscode.form(),
                                             getProspectiveEmail(),
                                             routes.AddPartnerContactDetailsEmailAddressController.displayPage(),
                                             routes.AddPartnerContactDetailsEmailAddressController.checkEmailVerificationCode()
        )
      )
    }

  def checkEmailVerificationCode(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok("TODO")
    }

  private def renderEnterEmailVerificationCodePage( // TODO will probably deduplicate into a base class
    form: Form[EmailAddressPasscode],
    prospectiveEmailAddress: String,
    backCall: Call,
    submitCall: Call
  )(implicit request: JourneyRequest[AnyContent]) =
    email_address_passcode_page(form, Some(prospectiveEmailAddress), backCall, submitCall)

}
