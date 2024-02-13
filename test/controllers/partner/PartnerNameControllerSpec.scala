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

import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import forms.partner.PartnerName
import models.registration.NewRegistrationUpdateService
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.partner.partner_name_page

class PartnerNameControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[partner_name_page]
  private val mcc  = stubMessagesControllerComponents()

  protected val mockNewRegistrationUpdater = new NewRegistrationUpdateService(mockRegistrationConnector)

  private val controller =
    new PartnerNameController(
      journeyAction = spyJourneyAction,
      registrationUpdater =
        mockNewRegistrationUpdater,
      config,
      soleTraderGrsConnector = mockSoleTraderGrsConnector,
      ukCompanyGrsConnector = mockUkCompanyGrsConnector,
      mockPartnershipGrsConnector,
      mockRegisteredSocietyGrsConnector,
      mcc = mcc,
      page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  private def registrationWithPartnershipDetailsAndInflightPartner =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(Some(aPartnershipPartner))

  private def registrationWithPartnershipDetailsAndInflightIncorporatedPartner =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(Some(aLimitedCompanyPartner))

  private val existingPartner =
    aPartnershipPartner

  private def registrationWithExistingPartner =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails.copy(partners = Seq(existingPartner)))))

  "PartnerNameController" should {

    "return 200" when {
      "user is authorised, a registration already exists with inflight partner" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)

        val result = controller.displayNewPartner()(FakeRequest())

        status(result) mustBe OK
      }

      "displaying an existing partner to edit their name" in {

        spyJourneyAction.setReg(registrationWithExistingPartner)

        val result = controller.displayExistingPartner(existingPartner.id)(FakeRequest())

        status(result) mustBe OK
      }
    }

    "update inflight registration" when {
      "user submits a valid name" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)
        mockRegistrationUpdate()
        mockCreatePartnershipGrsJourneyCreation("https://test/redirect/grs/identify-partnership")

        val result = controller.submitNewPartner()(postRequestEncoded(PartnerName("Test Partner")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("https://test/redirect/grs/identify-partnership")
        modifiedRegistration.inflightPartner.flatMap(_.partnerPartnershipDetails.flatMap(_.partnershipName)) mustBe Some("Test Partner")
      }

      "user submits an amendment to an existing partners name" in {

        spyJourneyAction.setReg(registrationWithExistingPartner)
        mockRegistrationUpdate()
        mockCreatePartnershipGrsJourneyCreation("https://test/redirect/grs/identify-partnership")

        val result = controller.submitExistingPartner(existingPartner.id)(postRequestEncoded(PartnerName("Test Partner")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("https://test/redirect/grs/identify-partnership")
        modifiedRegistration.findPartner(existingPartner.id).flatMap(_.partnerPartnershipDetails.flatMap(_.partnershipName)) mustBe Some("Test Partner")
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user does not enter name" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)

        val result =
          controller.submitNewPartner()(postRequestEncoded(PartnerName("")))

        status(result) mustBe BAD_REQUEST
      }

      "user enters a long name" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)

        val result = controller.submitNewPartner()(postRequestEncoded(PartnerName("abced" * 40)))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user tries to display an non existent partner" in {

        spyJourneyAction.setReg(registrationWithExistingPartner)

        intercept[RuntimeException](status(controller.displayExistingPartner("not-an-existing-partner-id")(FakeRequest())))
      }

      "user submits an amendment to a non existent partner" in {

        spyJourneyAction.setReg(registrationWithExistingPartner)
        mockRegistrationUpdate()

        intercept[RuntimeException](status(controller.submitExistingPartner("not-an-existing-partners-id")(postRequestEncoded(PartnerName("Amended name")))))
      }

      "user tries to set a name on a partner with a type which has a GRS provided name" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightIncorporatedPartner)

        intercept[RuntimeException](status(controller.displayNewPartner()(FakeRequest())))
      }

      "user tries to submit a name on a partner with a type which has a GRS provided name" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightIncorporatedPartner)
        mockRegistrationUpdate()

        intercept[RuntimeException](status(controller.submitNewPartner()(postRequestEncoded(PartnerName("Test Partner")))))
      }

      "user submits form and the registration update fails" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)
        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError](status(controller.submitNewPartner()(postRequestEncoded(PartnerName("Test Partner")))))
      }
    }
  }

}
