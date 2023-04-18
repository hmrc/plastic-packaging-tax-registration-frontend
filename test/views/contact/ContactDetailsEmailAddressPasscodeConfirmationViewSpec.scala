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

package views.contact

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import views.html.contact.email_address_passcode_confirmation_page

class ContactDetailsEmailAddressPasscodeConfirmationViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[email_address_passcode_confirmation_page]

  private val updateCall = Call("GET", "/update")

  "Email Address Passcode Confirmation View" should {

    val view = page(updateCall, Some(messages("primaryContactDetails.sectionHeader")))(
      request = registrationJourneyRequest,
      messages = messages
    )

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display title" in {

      view.select("title").text() must include(
        messages("primaryContactDetails.emailAddress.passcode.confirmation.title")
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("primaryContactDetails.sectionHeader")
      )
    }

    "display 'Continue' button" in {
      view.getElementById("submit").text() mustBe "Continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(updateCall, None)(request, messages)
    page.render(updateCall, None, request, messages)
  }

}
