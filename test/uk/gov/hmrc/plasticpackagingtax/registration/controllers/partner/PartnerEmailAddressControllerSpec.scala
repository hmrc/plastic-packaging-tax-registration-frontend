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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
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
  }

  override protected def afterEach(): Unit = {
    reset(page)
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

  private def registrationWithExistingPartner =
    aRegistration(
      withPartnershipDetails(Some(generalPartnershipDetails.copy(partners = Seq(existingPartner))))
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
      "user submits a valid email address for first partner and is prompted for email validation" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)
        mockRegistrationUpdate()
        when(mockEmailVerificationService.isEmailVerified(any(), any())(any())).thenReturn(
          Future.successful(false)
        )
        when(
          mockEmailVerificationService.sendVerificationCode(any(), any(), any())(any())
        ).thenReturn(Future.successful("an-email-verification-code"))

        val result = controller.submitNewPartner()(
          postRequestEncoded(EmailAddress("test@localhost"), saveAndContinueFormAction)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.PartnerEmailAddressController.confirmNewPartnerEmailCode().url
        )
        // TODO assert that the prospective email is been persisted
        // modifiedRegistration.inflightPartner.flatMap(_.contactDetails.flatMap(_.emailAddress))
      }

      //"user submits a valid email address for non nominated partner and is not prompted for email validation"

      "user submits an amendment to an existing partners email address" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)
        mockRegistrationUpdate()

        val result = controller.submitExistingPartner(existingPartner.id)(
          postRequestEncoded(EmailAddress("amended@localhost"))
        )

        status(result) mustBe SEE_OTHER

        modifiedRegistration.findPartner(existingPartner.id).flatMap(_.contactDetails).flatMap(
          _.emailAddress
        ) mustBe Some("amended@localhost")
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
