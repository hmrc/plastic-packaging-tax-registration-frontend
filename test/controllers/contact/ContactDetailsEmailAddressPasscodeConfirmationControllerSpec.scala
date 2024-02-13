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

package controllers.contact

import base.unit.ControllerSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.MockitoSugar.{reset, verify}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import views.html.contact.email_address_passcode_confirmation_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsEmailAddressPasscodeConfirmationControllerSpec extends ControllerSpec {
  private val page = mock[email_address_passcode_confirmation_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsEmailAddressPasscodeConfirmationController(journeyAction = spyJourneyAction, mcc = mcc, page = page)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val registration = aRegistration()
    spyJourneyAction.setReg(registration)
    given(page.apply(any(), any())(any(), any())).willReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Email passcode confirmation" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "display page with caption" when {
      "is group member" in {

        spyJourneyAction.setReg(aRegistration(withGroupDetail(Some(groupDetailsWithMembers))))

        await(controller.displayPage()(FakeRequest()))

        val captor = ArgumentCaptor.forClass(classOf[Option[String]])
        verify(page).apply(any(), captor.capture())(any(), any())
        captor.getValue mustBe Some("primaryContactDetails.group.sectionHeader")
      }

      "is single registration" in {

        spyJourneyAction.setReg(aRegistration())

        await(controller.displayPage()(FakeRequest()))

        val captor = ArgumentCaptor.forClass(classOf[Option[String]])
        verify(page).apply(any(), captor.capture())(any(), any())
        captor.getValue mustBe Some("primaryContactDetails.sectionHeader")
      }
    }

    "return 303" when {

      "when form is submitted" in {

        val result = controller.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
      }
    }

    "redirects to phone numbers page" when {
      "user submits answers" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()

        val result =
          controller.submit()(FakeRequest("POST", ""))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ContactDetailsTelephoneNumberController.displayPage().url)
      }
    }
  }

}
