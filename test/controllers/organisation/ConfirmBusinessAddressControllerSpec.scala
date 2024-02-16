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

package controllers.organisation

import base.unit.{AddressCaptureSpec, ControllerSpec}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import controllers.{routes => commonRoutes}
import forms.contact.Address
import forms.contact.Address.{NonUKAddress, UKAddress}
import play.api.test.FakeRequest
import views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ConfirmBusinessAddressControllerSpec extends ControllerSpec with AddressCaptureSpec {

  private val page = mock[confirm_business_address]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ConfirmBusinessAddressController(journeyAction = spyJourneyAction, registrationConnector = mockRegistrationConnector, mockAddressCaptureService, mcc = mcc, page = page)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Address], any(), any())(any(), any())).thenReturn(HtmlFormat.raw("business registered address"))

    mockRegistrationUpdate()
    simulateSuccessfulAddressCaptureInit(None)
    simulateValidAddressCapture()
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Confirm Company Address Controller" should {

    "display registered business address when it is populated and valid" in {
      spyJourneyAction.setReg(aRegistration())

      val resp = controller.displayPage()(FakeRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "business registered address"
    }

    "redirect to address capture" when {
      "registered business address is not present" in {
        val registration = aRegistration()
        spyJourneyAction.setReg(
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(businessRegisteredAddress = None)
          )
        )

        val resp = controller.displayPage()(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
      "registered business address is invalid" in {
        val registration   = aRegistration()
        val newAddressLine = "100 Really Long Street Name Which is Well in Excess of 35 characters"
        spyJourneyAction.setReg(
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(businessRegisteredAddress =
              registration.organisationDetails.businessRegisteredAddress.map {
                case addr: UKAddress     => addr.copy(addressLine1 = newAddressLine)
                case nonUK: NonUKAddress => nonUK.copy(addressLine1 = newAddressLine)
              }
            )
          )
        )

        val resp = controller.displayPage()(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

    "obtain address from address capture service and update registration and redirect to task list" when {
      "control is returned from address lookup frontend" in {
        val resp = controller.addressCaptureCallback()(FakeRequest())

        redirectLocation(resp) mustBe Some(commonRoutes.TaskListController.displayPage().url)

        modifiedRegistration.organisationDetails.businessRegisteredAddress mustBe Some(validCapturedAddress)
      }
    }

    "redirect to address lookup frontend" when {
      "user wants to change business address" in {
        spyJourneyAction.setReg(aRegistration())

        val resp = controller.changeBusinessAddress()(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

  }
}
