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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_name_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsNameViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[member_name_page]

  private val backLink   = Call("GET", "/back-link")
  private val updateLink = Call("PUT", "/update")

  private val organisationName = "Organisation"

  private def createView(form: Form[MemberName] = MemberName.form()): Document =
    page(form, organisationName, backLink, updateLink)(journeyRequest, messages)

  "Member name View" should {

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
        messages("contactDetails.member.name.title", organisationName)
      )
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

      view must haveGovukFieldError("firstName", "Enter first name")
    }

    "last name is not entered" in {

      val form = MemberName
        .form()
        .fillAndValidate(MemberName("first", ""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("lastName", "Enter last name")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(MemberName.form(), organisationName, backLink, updateLink)(journeyRequest, messages)
    page.render(MemberName.form(), organisationName, backLink, updateLink, journeyRequest, messages)
  }

}
