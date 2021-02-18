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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.FullName
import uk.gov.hmrc.plasticpackagingtax.registration.forms.FullName.allowedChars
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.full_name_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsFullNameViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[full_name_page]

  private def createView(form: Form[FullName] = FullName.form()): Document =
    page(form)(request, messages)

  "Primary Contact Details Full Name View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("primaryContactDetails.sectionHeader")
      messages must haveTranslationFor("primaryContactDetails.fullNamePage.title")
      messages must haveTranslationFor("primaryContactDetails.fullNamePage.hint")
      messages must haveTranslationFor("primaryContactDetails.fullNamePage.firstName")
      messages must haveTranslationFor("primaryContactDetails.fullNamePage.lastName")
    }

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(routes.RegistrationController.displayPage())
    }

    "display title" in {

      view.select("title").text() must include(messages("primaryContactDetails.fullNamePage.title"))
    }

    "display section header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("primaryContactDetails.sectionHeader")
      )
    }

    "display contact name question" in {

      view.getElementsByClass("govuk-fieldset__heading").text() must include(
        messages("primaryContactDetails.fullNamePage.title")
      )
    }

    "display question hint" in {

      view.getElementsByClass("govuk-inset-text").text() must include(
        messages("primaryContactDetails.fullNamePage.hint")
      )
    }

    "display first name and last name text input boxes" in {

      view must containElementWithID("firstName")
      view must containElementWithID("lastName")
    }

    "display 'Save and Continue' button" in {

      view.getElementById("submit").text() mustBe "Save and Continue"
    }

    "display 'Save and come back later' button" in {

      view.getElementById("save_and_come_back_later").text() mustBe "Save and come back later"
    }
  }

  "Primary Contact Details Full Name View when filled" should {

    "display data" in {

      val form = FullName
        .form()
        .fill(FullName("FirstName", "LastName"))
      val view = createView(form)

      view.getElementById("firstName").attr("value") mustBe "FirstName"
      view.getElementById("lastName").attr("value") mustBe "LastName"
    }

    "allow whitespace and special chars" in {

      val form = FullName
        .form()
        .fill(FullName("First Name " + allowedChars.get, "Last Name " + allowedChars.get))
      val view = createView(form)

      view.getElementById("firstName").attr("value") mustBe "First Name .-'"
      view.getElementById("lastName").attr("value") mustBe "Last Name .-'"
    }
  }

  "display error" when {

    "user did not enter first name or last name" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName("", ""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("firstName", "Enter a first name")
      view must haveGovukFieldError("lastName", "Enter a last name")

    }

    "user did entered non-alphabetic characters" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName("123", "321"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("firstName", "Enter a first name in the correct format")
      view must haveGovukFieldError("lastName", "Enter a last name in the correct format")

    }

    "user did entered more than 20 characters" in {
      val form = FullName
        .form()
        .fillAndValidate(
          FullName("averyveryveryveryverylongfirst", "averyveryveryveryverylonglast")
        )
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("firstName",
                                    "First name cannot be more than 20 characters long"
      )
      view must haveGovukFieldError("lastName", "Last name cannot be more than 20 characters long")

    }
  }
}
