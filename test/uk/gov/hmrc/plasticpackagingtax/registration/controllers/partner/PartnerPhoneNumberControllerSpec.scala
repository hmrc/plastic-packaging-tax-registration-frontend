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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.PhoneNumber
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_phone_number_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerPhoneNumberControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[partner_phone_number_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new PartnerPhoneNumberController(authenticate = mockAuthAction,
                                     journeyAction = mockJourneyAction,
                                     registrationConnector =
                                       mockRegistrationConnector,
                                     mcc = mcc,
                                     page = page
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

  private def registrationWithInflightPartnerWithMissingContactName = {
    val contactDetailsWithNoName =
      aLimitedCompanyPartner().contactDetails.map(_.copy(firstName = None, lastName = None))
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(
      Some(aLimitedCompanyPartner().copy(contactDetails = contactDetailsWithNoName))
    )
  }

  private def registrationWithPartnershipDetailsAndInflightPartnerWithContactNameAndPhoneNumber = {
    val contactDetailsWithPhoneNumber =
      aLimitedCompanyPartner().contactDetails.map(_.copy(phoneNumber = Some("12345678")))
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(
      Some(aLimitedCompanyPartner().copy(contactDetails = contactDetailsWithPhoneNumber))
    )
  }

  private val existingPartner = {
    val contactDetailsWithPhoneNumber =
      aLimitedCompanyPartner().contactDetails.map(_.copy(phoneNumber = Some("12345678")))
    aLimitedCompanyPartner().copy(contactDetails = contactDetailsWithPhoneNumber)
  }

  private def registrationWithExistingPartner =
    aRegistration(
      withPartnershipDetails(Some(generalPartnershipDetails.copy(partners = Seq(existingPartner))))
    )

  "PartnershipOtherPartnerPhoneNumberController" should {

    "return 200" when {
      "user is authorised, a registration already exists with already collected contact name and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)

        val result = controller.displayNewPartner()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists with already collected contact name and phone number display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)

        val result = controller.displayNewPartner()(getRequest())

        status(result) mustBe OK
      }

      "displaying an existing partner to edit their phone number" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)

        val result = controller.displayExistingPartner(existingPartner.id)(getRequest())

        status(result) mustBe OK
      }
    }

    "return 400" when {
      "user submits an invalid phone number" in {
        authorizedUser()
        mockRegistrationFind(
          registrationWithPartnershipDetailsAndInflightPartnerWithContactNameAndPhoneNumber
        )
        mockRegistrationUpdate()

        val result =
          controller.submitNewPartner()(
            postRequestEncoded(PhoneNumber("abcdef"), saveAndContinueFormAction)
          )

        status(result) mustBe BAD_REQUEST
      }
    }

    "update inflight registration" when {
      "user submits a valid phone number" in {
        authorizedUser()
        mockRegistrationFind(
          registrationWithPartnershipDetailsAndInflightPartnerWithContactNameAndPhoneNumber
        )
        mockRegistrationUpdate()

        val result = controller.submitNewPartner()(
          postRequestEncoded(PhoneNumber("12345678"), saveAndContinueFormAction)
        )

        status(result) mustBe SEE_OTHER

        modifiedRegistration.inflightPartner.flatMap(
          _.contactDetails.flatMap(_.phoneNumber)
        ) mustBe Some("12345678")
      }

      "user submits an amendment to an existing partners phone number" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)
        mockRegistrationUpdate()

        val result = controller.submitExistingPartner(existingPartner.id)(
          postRequestEncoded(PhoneNumber("987654321"), saveAndContinueFormAction)
        )

        status(result) mustBe SEE_OTHER

        modifiedRegistration.findPartner(existingPartner.id).flatMap(
          _.contactDetails.flatMap(_.phoneNumber)
        ) mustBe Some("987654321")
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
          postRequestEncoded(PhoneNumber("987654321"), saveAndContinueFormAction)
        )

        intercept[RuntimeException](status(result))
      }

      "user tries to submit before contact name for partner has been provided" in {
        authorizedUser()
        mockRegistrationFind(registrationWithInflightPartnerWithMissingContactName)

        val result =
          controller.submitNewPartner()(postRequest(Json.toJson(PhoneNumber("12345678"))))

        intercept[RuntimeException](status(result))
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)
        mockRegistrationUpdateFailure()
        val result =
          controller.submitNewPartner()(postRequest(Json.toJson(PhoneNumber("12345678"))))

        intercept[DownstreamServiceError](status(result))
      }
    }
  }

}
