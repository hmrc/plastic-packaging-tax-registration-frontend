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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.PhoneNumber
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.phone_number_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsPhoneNumberViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[phone_number_page]

  private val updateLink = Call("PUT", "/update")

  private def createView(form: Form[PhoneNumber] = PhoneNumber.form(), isGroup: Boolean = false): Document =
    page(form, updateLink, isGroup)(journeyRequest, messages)

  "Phone Number View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(
        messages("primaryContactDetails.phoneNumber.title",
                 journeyRequest.registration.primaryContactDetails.name.getOrElse(
                   messages("primaryContactDetails.fullName.default")
                 )
        )
      )
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

    "display hint" in {

      view.getElementById("value-hint").text() must include(
        messages("primaryContactDetails.phoneNumber.hint")
      )
    }

    "output hidden label correctly" in {
      view.getElementsByClass("govuk-visually-hidden").get(3).text() must include(
        messages("contactDetails.member.phoneNumber.title",
                 journeyRequest.registration.primaryContactDetails.name.getOrElse(
                   messages("primaryContactDetails.fullName.default")
                 )
        )
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

      val form = PhoneNumber
        .form()
        .fill(PhoneNumber("123"))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe "123"
    }
  }
  "display error" when {

    "phone number is not entered" in {

      val form = PhoneNumber
        .form()
        .fillAndValidate(PhoneNumber(""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("value", "Enter the telephone number of the main contact")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(PhoneNumber.form(), updateLink, false)(journeyRequest, messages)
    page.render(PhoneNumber.form(), updateLink, false, journeyRequest, messages)
  }

}
