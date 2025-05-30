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

package views.contact

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import forms.contact.EmailAddress
import views.html.contact.email_address_page

class ContactDetailsEmailAddressViewSpec extends UnitViewSpec with Matchers {

  private val page       = inject[email_address_page]
  private val updateLink = Call("PUT", "/update")

  private def createView(form: Form[EmailAddress] = EmailAddress.form(), isGroup: Boolean = false): Document =
    page(form, updateLink, isGroup)(registrationJourneyRequest, messages)

  private val mainContact = registrationJourneyRequest.registration.primaryContactDetails.name.get

  "Email Address View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("primaryContactDetails.emailAddress.title", mainContact))
    }

    "display section header" when {
      "Single organisation" in {
        view.getElementsByClass("govuk-caption-l").text() must include(messages("primaryContactDetails.sectionHeader"))
      }

      "Group organisation" in {
        val view = createView(isGroup = true)
        view.getElementsByClass("govuk-caption-l").text() must include("Representative member details")
        view.getElementsByClass("govuk-caption-l").text() must include(
          messages("primaryContactDetails.group.sectionHeader")
        )

      }
    }

    "display hint" in {

      view.getElementById("value-hint").text() must include(messages("primaryContactDetails.emailAddress.hint"))
    }

    "display email address label" in {

      view.getElementsByAttributeValueMatching("for", "value").text() must include(
        messages("primaryContactDetails.emailAddress.title", mainContact)
      )
    }

    "display email address input box" in {

      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Email address view when filled" should {

    "display data in email address input box" in {

      val form = EmailAddress
        .form()
        .fill(EmailAddress("test@test.com"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "test@test.com"
    }
  }
  "display error" when {

    "email address has not been entered" in {

      val form = EmailAddress
        .form()
        .fillAndValidate(EmailAddress(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter an email address")
    }

    "email address is not valid" in {

      val form = EmailAddress
        .form()
        .fillAndValidate(EmailAddress("test@"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter an email address in the correct format")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(EmailAddress.form(), updateLink, false)(registrationJourneyRequest, messages)
    page.render(EmailAddress.form(), updateLink, false, registrationJourneyRequest, messages)
  }

}
