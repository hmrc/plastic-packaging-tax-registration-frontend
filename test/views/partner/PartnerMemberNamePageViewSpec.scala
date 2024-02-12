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

package views.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.Call
import forms.group.MemberName
import views.html.partner.partner_member_name_page

class PartnerMemberNamePageViewSpec extends UnitViewSpec with Matchers {

  private val page             = inject[partner_member_name_page]
  private val updateLink       = Call("PUT", "/update")
  private val organisationName = "Organisation"
  private val nominated        = true
  private val notNominated     = false

  private def createViewNom(form: Form[MemberName] = MemberName.form()): Document =
    page(form, organisationName, nominated, updateLink)(registrationJourneyRequest, messages)

  private def createViewOther(form: Form[MemberName] = MemberName.form()): Document =
    page(form, organisationName, notNominated, updateLink)(registrationJourneyRequest, messages)

  "Member name viewNom" should {

    val viewNom   = createViewNom()
    val viewOther = createViewOther()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(viewNom) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(viewNom)

    }

    "display title" in {

      viewNom.select("title").text() must include(
        messages("partnership.otherPartners.contactNamePage.title", organisationName)
      )
    }

    "display member name input box" in {

      viewNom must containElementWithID("firstName")
      viewNom must containElementWithID("lastName")
    }

    "nominated" should {

      "display a caption" in {

        viewNom.getElementById("section-header").text() must include("Nominated partner details")

      }

      "display a paragraph text" in {

        viewNom.getElementsByClass("govuk-body").text() must include(
          "This is the name of the person who will manage the organisation’s Plastic Packaging Tax account, receive updates about the account and submit returns."
        )

      }

    }

    "other" should {

      "display a caption" in {

        viewOther.getElementById("section-header").text() must include("Other partner details")

      }

      "display a paragraph text" in {

        viewOther.getElementsByClass("govuk-body").text() must include(
          "Enter the correct contact here. This is usually the person responsible for the organisation’s Plastic Packaging Tax liability."
        )

      }
    }

    "display 'Save and continue' button" in {

      viewNom must containElementWithID("submit")
      viewNom.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Member name viewNom when filled" should {

    "display data in first name and last name input box" in {

      val form = MemberName
        .form()
        .fill(MemberName("Test", "User"))
      val viewNom = createViewNom(form)

      viewNom.getElementById("firstName").attr("value") mustBe "Test"
      viewNom.getElementById("lastName").attr("value") mustBe "User"
    }
  }

  "display error" when {

    "first name is not entered" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("", "last"))
      val viewNom = createViewNom(form)

      viewNom must haveGovukGlobalErrorSummary

      viewNom must haveGovukFieldError("firstName", "Enter the first name of the main contact")
    }

    "first name is spaces" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("  ", "last"))
      val viewNom = createViewNom(form)

      viewNom must haveGovukGlobalErrorSummary

      viewNom must haveGovukFieldError("firstName", "Enter a first name")
    }

    "first name is invalid" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("456465", "last"))
      val viewNom = createViewNom(form)

      viewNom must haveGovukGlobalErrorSummary

      viewNom must haveGovukFieldError(
        "firstName",
        "First name must only include letters, hyphens, spaces, apostrophes and full stops"
      )
    }

    "first name is > 35" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("x" * 36, "last"))
      val viewNom = createViewNom(form)

      viewNom must haveGovukGlobalErrorSummary

      viewNom must haveGovukFieldError("firstName", "First name must be 35 characters or fewer")
    }

    "last name is not entered" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", ""))
      val viewNom = createViewNom(form)

      viewNom must haveGovukGlobalErrorSummary

      viewNom must haveGovukFieldError("lastName", "Enter the last name of the main contact")
    }

    "last name is spaces" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", "   "))
      val viewNom = createViewNom(form)

      viewNom must haveGovukGlobalErrorSummary

      viewNom must haveGovukFieldError("lastName", "Enter a last name")
    }

    "last name is invalid" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", "1224"))
      val viewNom = createViewNom(form)

      viewNom must haveGovukGlobalErrorSummary

      viewNom must haveGovukFieldError(
        "lastName",
        "Last name must only include letters, hyphens, spaces, apostrophes and full stops"
      )
    }

    "last name is > 35" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", "x" * 36))
      val viewNom = createViewNom(form)
      viewNom must haveGovukGlobalErrorSummary

      viewNom must haveGovukFieldError("lastName", "Last name must be 35 characters or fewer")
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(MemberName.form(), organisationName, nominated, updateLink)(registrationJourneyRequest, messages)
    page.render(MemberName.form(), organisationName,nominated, updateLink, registrationJourneyRequest, messages)
  }

}
