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
    page(form)(journeyRequest, messages)

  "Primary Contact Details Full Name View" should {

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

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("primaryContactDetails.sectionHeader")
      )
    }

    "display contact name question" in {

      view.getElementsByClass("govuk-fieldset__heading").text() must include(
        messages("primaryContactDetails.fullNamePage.title")
      )
    }

    "display question hint" in {

      view.getElementsByClass("govuk-body").text() must include(
        messages("primaryContactDetails.fullNamePage.hint")
      )
    }

    "display full name text input boxes" in {

      view must containElementWithID("value")
    }

    "display 'Save and Continue' button" in {

      view.getElementById("submit").text() mustBe "Save and Continue"
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
        .fill(FullName("First Name " + allowedChars.get + " Last Name " + allowedChars.get))
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

      view must haveGovukFieldError("value", "Enter a name")
    }

    "user entered non-alphabetic characters" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName("123 321"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter a name in the correct format")
    }

    "user entered more than 160 characters" in {
      val form = FullName
        .form()
        .fillAndValidate(FullName("abcde" * 40))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Name cannot be more than 160 characters long")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(FullName.form())(request, messages)
    page.render(FullName.form(), request, messages)
  }

}
