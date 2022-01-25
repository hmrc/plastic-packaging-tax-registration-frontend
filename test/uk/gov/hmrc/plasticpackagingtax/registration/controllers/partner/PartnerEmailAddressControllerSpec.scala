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
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_email_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerEmailAddressControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[partner_email_address_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new PartnerEmailAddressController(authenticate = mockAuthAction,
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

  "PartnershipOtherPartnerEmailAddressController" should {

    "return 200" when {
      "user is authorised, a registration already exists with already collected contact name and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists with already collected contact name and email address display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(
          registrationWithPartnershipDetailsAndInflightPartnerWithContactNameAndEmailAddress
        )

        val result = controller.displayPage()(getRequest())

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
      "user submits a valid email address" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)
        mockRegistrationUpdate()

        val result = controller.submit()(
          postRequestEncoded(EmailAddress("test@localhost"), saveAndContinueFormAction)
        )

        status(result) mustBe SEE_OTHER

        modifiedRegistration.inflightPartner.flatMap(
          _.contactDetails.flatMap(_.emailAddress)
        ) mustBe Some("test@localhost")
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()

        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user is authorised but does not have an inflight journey and display page method is invoked" in {
        authorizedUser()

        val result = controller.displayPage()(getRequest())

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
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartnerWithContactName)
        mockRegistrationUpdateFailure()

        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[DownstreamServiceError](status(result))
      }
    }
  }

}
