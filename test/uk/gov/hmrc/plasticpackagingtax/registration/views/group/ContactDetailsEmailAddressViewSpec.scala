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

package uk.gov.hmrc.plasticpackagingtax.registration.views.group

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_email_address_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsEmailAddressViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[member_email_address_page]

  private val backLink   = Call("GET", "/back-link")
  private val updateLink = Call("PUT", "/update")

  private val contactName = Some("Test")

  private def createView(form: Form[EmailAddress] = EmailAddress.form()): Document =
    page(form, contactName, backLink, updateLink)(journeyRequest, messages)

  "Phone Number View" should {

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
        messages("contactDetails.member.emailAddress.title",
                 contactName.getOrElse(messages("primaryContactDetails.fullName.default"))
        )
      )
    }

    "display hint" in {

      view.getElementById("value-hint").text() must include(
        messages("contactDetails.member.emailAddress.hint")
      )
    }

    "display phone number input box" in {

      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Phone Number View when filled" should {

    "display data in phone number input box" in {

      val form = EmailAddress
        .form()
        .fill(EmailAddress("test@test.com"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "test@test.com"
    }
  }
  "display error" when {

    "email address is not entered" in {

      val form = EmailAddress
        .form()
        .fillAndValidate(EmailAddress(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter an email address")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(EmailAddress.form(), contactName, backLink, updateLink)(journeyRequest, messages)
    page.render(EmailAddress.form(), contactName, backLink, updateLink, journeyRequest, messages)
  }

}
