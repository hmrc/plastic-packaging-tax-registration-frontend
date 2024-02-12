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

package views.contact

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import forms.contact.FullName
import views.html.contact.full_name_page

class ContactDetailsFullNameViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[full_name_page]
  private val updateLink = Call("PUT", "/update")

  private def createView(form: Form[FullName] = FullName.form(), isGroup: Boolean = false): Document =
    page(form, updateLink, isGroup)(registrationJourneyRequest, messages)

  "Primary Contact Details Full Name View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {
      view.select("title").text() must include(messages("primaryContactDetails.fullNamePage.title"))
    }

    "display section header" when {
      "Single organisation" in {
        view.getElementsByClass("govuk-caption-l").text() must include(
          messages("primaryContactDetails.sectionHeader")
        )
      }

      "Group organisation" in {
        val view = createView(isGroup = true)
        view.getElementsByClass("govuk-caption-l").text() must include(
          "Representative member details"
        )
        view.getElementsByClass("govuk-caption-l").text() must include(
         messages("primaryContactDetails.group.sectionHeader")
        )

      }
    }

    "display contact name question" in {
      val view = createView(isGroup = true)

      view.getElementsByAttributeValueMatching("for", "value").text() must include(
        messages("primaryContactDetails.fullNamePage.title")
      )
    }

    "display question hint" in {
      view.getElementById("value-hint").text() must include(
        messages("primaryContactDetails.fullNamePage.hint")
      )
    }

    "display full name text input boxes" in {

      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {

      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Primary Contact Details Full Name View when filled" should {

    "display data" in {

      val form = FullName
        .form()
        .fill(FullName("FirstName LastName"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "FirstName LastName"
    }

    "allow whitespace and special chars" in {

      val form = FullName
        .form()
        .fill(FullName("First Name .-' " + "Last Name" + " .-'"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "First Name .-' Last Name .-'"
    }
  }

  "display error" when {

    "user did not enter name" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter the name of the main contact")
    }

    "user entered all spaces" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName("   "))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter a name")
    }

    "user entered non-alphabetic characters" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName("123 321"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError(
        "value",
        "Name must only include letters, hyphens, spaces, apostrophes and full stops"
      )
    }

    "user entered more than 160 characters" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName("x" * 161))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Name must be 160 characters or fewer")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(FullName.form(), updateLink, false)(request, messages)
    page.render(FullName.form(), updateLink, false, request, messages)
  }

}
