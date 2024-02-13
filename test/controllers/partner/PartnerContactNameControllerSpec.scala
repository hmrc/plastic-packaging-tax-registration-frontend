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
import forms.group.MemberName
import models.registration.NewRegistrationUpdateService
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.partner.partner_member_name_page

class PartnerContactNameControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[partner_member_name_page]
  private val mcc  = stubMessagesControllerComponents()

  protected val mockNewRegistrationUpdater = new NewRegistrationUpdateService(mockRegistrationConnector)

  private val controller =
    new PartnerContactNameController(
      journeyAction = spyJourneyAction,
      registrationUpdateService =
        mockNewRegistrationUpdater,
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

  private def registrationWithPartnershipDetailsAndInflightPartner =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(Some(aLimitedCompanyPartner))

  private def registrationWithPartnershipDetailsAndNonNominatedInflightPartner =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).addOtherPartner(aSoleTraderPartner).withInflightPartner(Some(aPartnershipPartner))

  private val existingPartner =
    aLimitedCompanyPartner

  private def registrationWithExistingPartner =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails.copy(partners = Seq(existingPartner)))))

  "PartnerContactNameController" should {

    "return 200" when {
      "user is authorised, a registration already exists with already collected nominated partner" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)

        val result = controller.displayNewPartner()(FakeRequest())

        status(result) mustBe OK
      }

      "displaying an existing partner to edit their contact name" in {

        spyJourneyAction.setReg(registrationWithExistingPartner)

        val result = controller.displayExistingPartner(existingPartner.id)(FakeRequest())

        status(result) mustBe OK
      }
    }

    "update inflight registration" when {
      "user submits a complete contact name" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndNonNominatedInflightPartner)
        mockRegistrationUpdate()

        val result = controller.submitNewPartner()(postRequestEncoded(MemberName("John", "Smith")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.PartnerEmailAddressController.displayNewPartner().url)

        modifiedRegistration.inflightPartner.flatMap(_.contactDetails.flatMap(_.firstName)) mustBe Some("John")
        modifiedRegistration.inflightPartner.flatMap(_.contactDetails.flatMap(_.lastName)) mustBe Some("Smith")
      }

      "nominated partner submits a contact name and is prompted for job title" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)
        mockRegistrationUpdate()

        val result = controller.submitNewPartner()(postRequestEncoded(MemberName("John", "Smith")))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.PartnerJobTitleController.displayNewPartner().url)
      }

      "user submits an amendment to an existing partners contact name" in {

        spyJourneyAction.setReg(registrationWithExistingPartner)
        mockRegistrationUpdate()

        val result = controller.submitExistingPartner(existingPartner.id)(postRequestEncoded(MemberName("Jane", "Smith")))

        status(result) mustBe SEE_OTHER

        val modifiedContactDetails =
          modifiedRegistration.findPartner(existingPartner.id).flatMap(_.contactDetails)
        modifiedContactDetails.flatMap(_.firstName) mustBe Some("Jane")
        modifiedContactDetails.flatMap(_.lastName) mustBe Some("Smith")
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user does not enter name" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)

        val result =
          controller.submitNewPartner()(postRequestEncoded(MemberName("John", "")))

        status(result) mustBe BAD_REQUEST
      }

      "user enters a long name" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)

        val result = controller.submitNewPartner()(postRequestEncoded(MemberName("abced" * 40, "Smith")))

        status(result) mustBe BAD_REQUEST
      }

      "user enters non-alphabetic characters" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)

        val result =
          controller.submitNewPartner()(postRequestEncoded(MemberName("FirstNam807980234£$", "LastName")))

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

        intercept[RuntimeException](status(controller.submitExistingPartner("not-an-existing-partners-id")(postRequestEncoded(MemberName("Jane", "Smith")))))
      }

      "user submits form and the registration update fails" in {

        spyJourneyAction.setReg(registrationWithPartnershipDetailsAndInflightPartner)
        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError](status(controller.submitNewPartner()(postRequestEncoded(MemberName("John", "Smith")))))
      }
    }
  }

}
