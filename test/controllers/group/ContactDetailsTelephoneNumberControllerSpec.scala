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

package controllers.group

import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import controllers.group.{routes => groupRoutes}
import forms.contact.PhoneNumber
import models.registration.NewRegistrationUpdateService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.group.member_phone_number_page

class ContactDetailsTelephoneNumberControllerSpec extends ControllerSpec with DefaultAwaitTimeout with PptTestData {

  private val page = mock[member_phone_number_page]
  private val mcc  = stubMessagesControllerComponents()

  private val mockNewRegistrationUpdater = new NewRegistrationUpdateService(mockRegistrationConnector)

  private val controller =
    new ContactDetailsTelephoneNumberController(
      journeyAction = spyJourneyAction,
      mcc = mcc,
      page = page,
      registrationUpdater = mockNewRegistrationUpdater
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "ContactDetailsTelephoneNumberController" should {

    "return 200" when {

      "user is authorised display page method is invoked" in {

        val member = groupMember.copy(contactDetails = None)
        spyJourneyAction.setReg(aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(member))))))
        val result = controller.displayPage(groupMember.id)(FakeRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {

        spyJourneyAction.setReg(aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember))))))
        val result = controller.displayPage(groupMember.id)(FakeRequest())

        status(result) mustBe OK
      }
    }

    "return 303 (OK)" when {
      "user submits the phone number" in {

        spyJourneyAction.setReg(aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember))))))
        mockRegistrationUpdate()

        val result =
          controller.submit(groupMember.id)(postRequestEncoded(PhoneNumber("077123")))

        status(result) mustBe SEE_OTHER
        modifiedRegistration.groupDetail.get.members.lastOption.get.contactDetails.get.phoneNumber mustBe Some("077123")
        redirectLocation(result) mustBe Some(
          groupRoutes.ContactDetailsConfirmAddressController.displayPage(groupMember.id).url
        )
        reset(mockRegistrationConnector)
      }
    }

    "return pre populated form" when {

      def pageForm: Form[PhoneNumber] = {
        val captor = ArgumentCaptor.forClass(classOf[Form[PhoneNumber]])
        verify(page).apply(captor.capture(), any(), any())(any(), any())
        captor.getValue
      }

      "data exist" in {

        spyJourneyAction.setReg(aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember))))))

        await(controller.displayPage(groupMember.id)(FakeRequest()))

        pageForm.get.value mustBe "077123"
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid phone number" in {

        val result = controller.submit(groupMember.id)(postRequest(Json.toJson(PhoneNumber("$%^"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user submits form and the registration update fails" in {

        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError](
          status(controller.submit(groupMember.id)(postRequestEncoded(PhoneNumber("077123"))))
        )
      }

      "user submits form and a registration update runtime exception occurs" in {

        mockRegistrationException()

        intercept[RuntimeException](
          status(controller.submit(groupMember.id)(postRequestEncoded(PhoneNumber("077123"))))
        )
      }
    }
  }
}
