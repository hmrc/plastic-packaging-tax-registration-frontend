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

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.{
  EmailAddress,
  EmailAddressPasscode
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationJourneyStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.NewRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.services.EmailVerificationService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.{
  email_address_passcode_confirmation_page,
  email_address_passcode_page,
  too_many_attempts_passcode_page
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_email_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class PartnerEmailAddressControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[partner_email_address_page]

  private val email_address_passcode_page =
    mock[email_address_passcode_page]

  private val too_many_attempts_passcode_page = mock[too_many_attempts_passcode_page]

  private val emailCorrectPasscodePage = mock[email_address_passcode_confirmation_page]

  private val mcc = stubMessagesControllerComponents()

  protected val mockNewRegistrationUpdater = new NewRegistrationUpdateService(
    mockRegistrationConnector
  )

  private val mockEmailVerificationService = mock[EmailVerificationService]

  private val controller =
    new PartnerEmailAddressController(authenticate = mockAuthAction,
                                      journeyAction = mockJourneyAction,
                                      registrationUpdateService =
                                        mockNewRegistrationUpdater,
                                      mcc = mcc,
                                      page = page,
                                      emailPasscodePage = email_address_passcode_page,
                                      emailCorrectPasscodePage = emailCorrectPasscodePage,
                                      emailIncorrectPasscodeTooManyAttemptsPage =
                                        too_many_attempts_passcode_page,
                                      emailVerificationService = mockEmailVerificationService
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
    when(email_address_passcode_page.apply(any(), any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.empty
    )
    when(emailCorrectPasscodePage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    reset(email_address_passcode_page)
    reset(emailCorrectPasscodePage)
    super.afterEach()
  }

  private def registrationWithPartnershipDetailsAndInflightPartnerWithContactName =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(
      Some(aLimitedCompanyPartner())
    )

  private def registrationWithPartnershipDetailsAndInflightPartnerWithContactNameAndEmailAddress = {
    val contactDetailsWithEmailAddress =
      aLimitedCompanyPartner().contactDetails.map(_.copy(emailAddress = Some("test@localhost")))
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(
      Some(aLimitedCompanyPartner().copy(contactDetails = contactDetailsWithEmailAddress))
    )
  }

  private val existingPartner =
    aLimitedCompanyPartner()

  val nonNominatedExistingPartner = aSoleTraderPartner()

  private def registrationWithExistingPartner =
    aRegistration(
      withPartnershipDetails(Some(generalPartnershipDetails.copy(partners = Seq(existingPartner))))
    )

  private def registrationWithExistingPartners =
    aRegistration(
      withPartnershipDetails(
        Some(
          generalPartnershipDetails.copy(partners =
            Seq(existingPartner, nonNominatedExistingPartner)
          )
        )
      )
    )

  private def registrationWithExistingPartnerAndInflightPartner =
    aRegistration(
      withPartnershipDetails(Some(generalPartnershipDetails.copy(partners = Seq(existingPartner))))
    ).withInflightPartner(Some(aSoleTraderPartner()))

  "PartnerEmailAddressController" should {

    "return 200" when {
      "user is authorised, a registration already exists with already collected contact name and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)

        val result = controller.displayNewPartner()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists with already collected contact name and email address display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(
          registrationWithPartnershipDetailsAndInflightPartnerWithContactNameAndEmailAddress
        )

        val result = controller.displayNewPartner()(getRequest())

        status(result) mustBe OK
      }

      "displaying an existing partner to edit their contact name" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)

        val result = controller.displayExistingPartner(existingPartner.id)(getRequest())

        status(result) mustBe OK
      }
    }

    "update inflight registration" when {
      "user submits a valid email address for first partner and is sent for email validation" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)
        mockRegistrationUpdate()
        when(mockEmailVerificationService.isEmailVerified(any(), any())(any())).thenReturn(
          Future.successful(false)
        )
        when(
          mockEmailVerificationService.sendVerificationCode(any(), any(), any())(any())
        ).thenReturn(Future.successful("an-email-verification-journey-id"))

        val result = controller.submitNewPartner()(
          postRequestEncoded(EmailAddress("proposed-email@localhost"), saveAndContinueFormAction)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.PartnerEmailAddressController.confirmNewPartnerEmailCode().url
        )

        // Assert that the detail of the email verification journey were stashed in the expected place
        modifiedRegistration.primaryContactDetails.journeyId mustBe Some(
          "an-email-verification-journey-id"
        )
        modifiedRegistration.primaryContactDetails.prospectiveEmail mustBe Some(
          "proposed-email@localhost"
        )
      }

      "user is prompted to enter email verification code" in {
        authorizedUser()
        val primaryContactDetailsWithEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("an-email@localhost")
          )
        val withEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.copy(
            primaryContactDetails = primaryContactDetailsWithEmailVerificationJourney
          )

        mockRegistrationFind(withEmailVerificationJourney)

        val result = controller.confirmNewPartnerEmailCode()(getRequest())

        status(result) mustBe OK
      }

      "user submits correct email verification code" in {
        authorizedUser()
        val primaryContactDetailsWithEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("an-email@localhost")
          )
        val withEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.copy(
            primaryContactDetails = primaryContactDetailsWithEmailVerificationJourney
          )
        mockRegistrationFind(withEmailVerificationJourney)

        // Email verification will be called to check the user submitted code
        when(
          mockEmailVerificationService.checkVerificationCode(
            ArgumentMatchers.eq("ACODE"),
            ArgumentMatchers.eq("an-email@localhost"),
            ArgumentMatchers.eq("email-verification-journey-id")
          )(any())
        ).thenReturn(Future.successful(EmailVerificationJourneyStatus.COMPLETE))

        val result = controller.checkNewPartnerEmailVerificationCode()(
          postRequestEncoded(EmailAddressPasscode("ACODE"), saveAndContinueFormAction)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.PartnerEmailAddressController.emailVerifiedNewPartner().url
        )
      }

      "user submits blank verification code" in {
        authorizedUser()
        val primaryContactDetailsWithEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("an-email@localhost")
          )
        val withEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.copy(
            primaryContactDetails = primaryContactDetailsWithEmailVerificationJourney
          )
        mockRegistrationFind(withEmailVerificationJourney)

        // Email verification will not be called in this case
        val result = controller.checkNewPartnerEmailVerificationCode()(
          postRequestEncoded(EmailAddressPasscode(""), saveAndContinueFormAction)
        )

        status(result) mustBe BAD_REQUEST
      }

      "user is prompted for confirm they still want to apply the verified email address" in {
        authorizedUser()
        val primaryContactDetailsWithEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("an-email@localhost")
          )
        val withEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.copy(
            primaryContactDetails = primaryContactDetailsWithEmailVerificationJourney
          )

        mockRegistrationFind(withEmailVerificationJourney)
        mockRegistrationUpdate()

        val result = controller.emailVerifiedNewPartner()(getRequest())

        status(result) mustBe OK
      }

      "user submits a valid email address for non nominated partner has it accepted immediately without verification" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartnerAndInflightPartner)
        mockRegistrationUpdate()

        val result = controller.submitNewPartner()(
          postRequestEncoded(EmailAddress("new-partners-email@localhost"),
                             saveAndContinueFormAction
          )
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.PartnerPhoneNumberController.displayNewPartner().url
        )

        modifiedRegistration.inflightPartner.flatMap(
          _.contactDetails.flatMap(_.emailAddress)
        ) mustBe Some("new-partners-email@localhost")
      }

      "user submits confirmation of verified email address and has it updated" in {
        authorizedUser()
        val primaryContactDetailsWithEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.primaryContactDetails.copy(
            journeyId = Some("email-verification-journey-id"),
            prospectiveEmail = Some("an-email@localhost")
          )
        val withEmailVerificationJourney =
          registrationWithPartnershipDetailsAndInflightPartnerWithContactName.copy(
            primaryContactDetails = primaryContactDetailsWithEmailVerificationJourney
          )

        mockRegistrationFind(withEmailVerificationJourney)
        mockRegistrationUpdate()

        val result = controller.confirmEmailUpdateNewPartner()(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.PartnerPhoneNumberController.displayNewPartner().url
        )

        modifiedRegistration.inflightPartner.flatMap(
          _.contactDetails.flatMap(_.emailAddress)
        ) mustBe Some("an-email@localhost")
      }

      "user submits an amendment to an existing partners email address" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartners)
        mockRegistrationUpdate()

        val result = controller.submitExistingPartner(nonNominatedExistingPartner.id)(
          postRequestEncoded(EmailAddress("amended@localhost"))
        )

        status(result) mustBe SEE_OTHER

        modifiedRegistration.findPartner(nonNominatedExistingPartner.id).flatMap(
          _.contactDetails
        ).flatMap(_.emailAddress) mustBe Some("amended@localhost")
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()

        val result = controller.displayNewPartner()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user is authorised but does not have an inflight journey and display page method is invoked" in {
        authorizedUser()

        val result = controller.displayNewPartner()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user tries to display an non existent partner" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)

        val result = controller.displayExistingPartner("not-an-existing-partner-id")(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user submits an amendment to a non existent partner" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)
        mockRegistrationUpdate()

        val result = controller.submitExistingPartner("not-an-existing-partners-id")(
          postRequestEncoded(EmailAddress("test@localhost"), saveAndContinueFormAction)
        )

        intercept[RuntimeException](status(result))
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartnerAndInflightPartner)
        mockRegistrationUpdateFailure()

        val result =
          controller.submitNewPartner()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[DownstreamServiceError](status(result))
      }
    }
  }

}
