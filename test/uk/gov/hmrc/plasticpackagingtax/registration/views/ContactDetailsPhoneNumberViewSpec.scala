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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PhoneNumber
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.phone_number_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsPhoneNumberViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[phone_number_page]

  private def createView(form: Form[PhoneNumber] = PhoneNumber.form()): Document =
    page(form)(journeyRequest, messages)

  "Phone Number View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        routes.ContactDetailsEmailAddressController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(
        messages("primaryContactDetails.phoneNumber.title",
                 journeyRequest.registration.primaryContactDetails.name.getOrElse(
                   messages("primaryContactDetails.phoneNumber.default")
                 )
        )
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("primaryContactDetails.sectionHeader")
      )
    }

    "display hint" in {

      view.getElementById("value-hint").text() must include(
        messages("primaryContactDetails.phoneNumber.hint")
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

      view must haveGovukFieldError("value", "Enter a phone number")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(PhoneNumber.form())(journeyRequest, messages)
    page.render(PhoneNumber.form(), journeyRequest, messages)
  }

}
