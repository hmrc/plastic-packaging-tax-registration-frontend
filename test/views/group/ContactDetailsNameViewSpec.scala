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

package views.group

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import forms.group.MemberName
import views.html.group.member_name_page

class ContactDetailsNameViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[member_name_page]

  private val updateLink = Call("PUT", "/update")

  private val organisationName = "Organisation"

  private def createView(form: Form[MemberName] = MemberName.form()): Document =
    page(form, organisationName, updateLink, groupMember.id)(registrationJourneyRequest, messages)

  "Member name View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("contactDetails.member.name.title"))
    }

    "display hint" in {

      view.getElementsByClass("govuk-body").text() must include(
        messages("contactDetails.member.name.hint")
      )
    }

    "display member name input box" in {

      view must containElementWithID("firstName")
      view must containElementWithID("lastName")
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Member name view when filled" should {

    "display data in first name and last name input box" in {

      val form = MemberName
        .form()
        .fill(MemberName("Test", "User"))
      val view = createView(form)

      view.getElementById("firstName").attr("value") mustBe "Test"
      view.getElementById("lastName").attr("value") mustBe "User"
    }
  }
  "display error" when {

    "first name is not entered" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("", "last"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("firstName", "Enter the first name of the main contact")
    }

    "first name is spaces" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("  ", "last"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("firstName", "Enter a first name")
    }

    "first name is invalid" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("456465", "last"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError(
        "firstName",
        "First name must only include letters, hyphens, spaces, apostrophes and full stops"
      )
    }

    "first name is > 35" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("x" * 36, "last"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("firstName", "First name must be 35 characters or fewer")
    }

    "last name is not entered" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", ""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("lastName", "Enter the last name of the main contact")
    }

    "last name is spaces" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", "   "))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("lastName", "Enter a last name")
    }

    "last name is invalid" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", "1224"))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError(
        "lastName",
        "Last name must only include letters, hyphens, spaces, apostrophes and full stops"
      )
    }

    "last name is > 35" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", "x" * 36))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("lastName", "Last name must be 35 characters or fewer")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(MemberName.form(), organisationName, updateLink, groupMember.id)(
      registrationJourneyRequest,
      messages
    )
    page.render(MemberName.form(),
                organisationName,
                updateLink,
                groupMember.id,
                registrationJourneyRequest,
                messages
    )
  }

}
