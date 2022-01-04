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

package uk.gov.hmrc.plasticpackagingtax.registration.views.contact

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.email_address_passcode_confirmation_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsEmailAddressPasscodeConfirmationViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[email_address_passcode_confirmation_page]

  private val backLink   = Call("GET", "/back")
  private val updateCall = Call("GET", "/update")

  "Email Address Passcode Confirmation View" should {

    val view = page(backLink, updateCall)(request = journeyRequest, messages = messages)

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

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(backLink.url)
    }

    "display 'Continue' button" in {
      view.getElementById("submit").text() mustBe "Continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(backLink, updateCall)(request, messages)
    page.render(backLink, updateCall, request, messages)
  }

}
