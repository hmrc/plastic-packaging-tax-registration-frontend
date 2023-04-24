/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.contact

import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import forms.contact.PhoneNumber
import models.registration.PrimaryContactDetails
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.contact.phone_number_page

class ContactDetailsTelephoneNumberControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[phone_number_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsTelephoneNumberController(journeyAction = spyJourneyAction,
                                                registrationConnector = mockRegistrationConnector,
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

  "ContactDetailsTelephoneNumberController" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        spyJourneyAction.setReg(aRegistration())

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {

        spyJourneyAction.setReg(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "show page fo0r a group member" in {

      spyJourneyAction.setReg(aRegistration(withGroupDetail(Some(groupDetailsWithMembers))))

      await(controller.displayPage()(getRequest()))

      val captor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(page).apply(any(), any(), captor.capture())(any(), any())
      captor.getValue mustBe true
    }

      "return 303 (OK)" when {
        "user submits the phone number" in {

          spyJourneyAction.setReg(aRegistration())
          mockRegistrationUpdate()

          val result =
            controller.submit()(postRequestEncoded(PhoneNumber("077123")))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.primaryContactDetails.phoneNumber mustBe Some("077123")

          redirectLocation(result) mustBe Some(
            routes.ContactDetailsConfirmAddressController.displayPage().url
          )

          reset(mockRegistrationConnector)
        }
      }


    "return prepopulated form" when {

      def pageForm: Form[PhoneNumber] = {
        val captor = ArgumentCaptor.forClass(classOf[Form[PhoneNumber]])
        verify(page).apply(captor.capture(), any(), any())(any(), any())
        captor.getValue
      }

      "data exist" in {

        spyJourneyAction.setReg(
          aRegistration(
            withPrimaryContactDetails(PrimaryContactDetails(phoneNumber = Some("077123")))
          )
        )

        await(controller.displayPage()(getRequest()))

        pageForm.get.value mustBe "077123"
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid phone number" in {

        val result = controller.submit()(postRequest(Json.toJson(PhoneNumber("$%^"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user submits form and the registration update fails" in {

        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError](status(
          controller.submit()(postRequest(Json.toJson(PhoneNumber("077123"))))
        ))
      }

      "user submits form and a registration update runtime exception occurs" in {

        mockRegistrationException()

        intercept[RuntimeException](status(
          controller.submit()(postRequest(Json.toJson(PhoneNumber("077123"))))
        ))
      }
    }
  }
}
