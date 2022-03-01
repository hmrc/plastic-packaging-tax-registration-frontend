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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddressPasscode
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.email_address_passcode_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsEmailAddressPasscodeViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[email_address_passcode_page]

  private val backLink   = Call("GET", "/back")
  private val updateCall = Call("GET", "/update")

  private def createView(
    form: Form[EmailAddressPasscode] = EmailAddressPasscode.form(),
    emailAddress: String = "test@test.com"
  ): Document =
    page(form,
         Some(emailAddress),
         backLink,
         updateCall,
         Some(messages("primaryContactDetails.sectionHeader"))
    )(journeyRequest, messages)

  "Email Address Passcode View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(backLink.url)
    }

    "display title" in {

      view.select("title").text() must include(
        messages("primaryContactDetails.emailAddress.passcode.title")
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("primaryContactDetails.sectionHeader")
      )
    }

    "display hint" in {

      view.getElementById("value-hint").text() must include(
        messages("primaryContactDetails.emailAddress.passcode.hint")
      )
    }

    "display email address passcode label" in {

      view.getElementsByAttributeValueMatching("for", "value").text() must include(
        messages("primaryContactDetails.emailAddress.passcode.label")
      )
    }

    "display email address passcode question" in {

      view.getElementsByClass("govuk-heading-l").text() must include(
        messages("primaryContactDetails.emailAddress.passcode.title")
      )
    }

    "display email address passcode detail" in {

      view.getElementsByClass("govuk-body").get(0).text() mustBe
        messages("primaryContactDetails.emailAddress.passcode.detail", "to test@test.com")

    }

    "display email address passcode summary detail" in {
      val summaryDetails = view.getElementsByClass("govuk-body")
      summaryDetails.get(1).text() mustBe messages(
        "primaryContactDetails.emailAddress.passcode.summary.detail"
      )
      summaryDetails.get(2).text() must include(
        messages("primaryContactDetails.emailAddress.passcode.summary.detail.2",
                 "provide a different email address"
        )
      )
    }

    "display email address passcode input box" in {

      view must containElementWithID("value")
    }

    "display 'Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Continue"
    }

  }

  "Email address view when filled" should {

    "display data in email address passcode input box" in {

      val form = EmailAddressPasscode
        .form()
        .fill(EmailAddressPasscode("DNCLRK"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "DNCLRK"
    }
  }
  "display error" when {

    "email address passcode has not been entered" in {

      val form = EmailAddressPasscode
        .form()
        .fillAndValidate(EmailAddressPasscode(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter a passcode")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(EmailAddressPasscode.form(), Some("test@test.com"), backLink, updateCall, None)(request,
                                                                                           messages
    )
    page.render(EmailAddressPasscode.form(),
                Some("test@test.com"),
                backLink,
                updateCall,
                Some(messages("primaryContactDetails.sectionHeader")),
                request,
                messages
    )
  }

}
