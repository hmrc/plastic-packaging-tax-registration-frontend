/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.email_address_passcode_confirmation_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsEmailAddressPasscodeConfirmationViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[email_address_passcode_confirmation_page]

  "Email Address Passcode Confirmation View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("primaryContactDetails.sectionHeader")
      messages must haveTranslationFor(
        "primaryContactDetails.emailAddress.passcode.confirmation.title"
      )
      messages must haveTranslationFor(
        "primaryContactDetails.emailAddress.passcode.confirmation.privacyNotice"
      )
      messages must haveTranslationFor(
        "primaryContactDetails.emailAddress.passcode.confirmation.privacyNotice.link"
      )
    }

    val view = page()(request = journeyRequest, messages = messages)

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display title" in {

      view.select("title").text() must include(
        messages("primaryContactDetails.emailAddress.passcode.confirmation.title")
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("primaryContactDetails.sectionHeader")
      )
    }

    "display hint" in {

      view.getElementById("email-address-passcode-confirmation-id").text() must include(
        messages("primaryContactDetails.emailAddress.passcode.confirmation")
      )
    }

    "display 'Privacy' Notice" in {

      view.getElementsByClass("govuk-body").get(1).text() must include(
        messages("primaryContactDetails.emailAddress.passcode.confirmation.privacyNotice",
                 messages(
                   "primaryContactDetails.emailAddress.passcode.confirmation.privacyNotice.link"
                 )
        )
      )
    }

    "display 'Continue' button" in {
      view.getElementById("submit").text() mustBe "Continue"
    }

  }

}
