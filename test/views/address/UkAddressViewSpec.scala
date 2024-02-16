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

package views.address

import base.unit.UnitViewSpec
import forms.address.UkAddressForm
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import views.html.address.uk_address_page

class UkAddressViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[uk_address_page]

  "UK Address View" should {

    def createView(form: Form[Boolean]): Document =
      page(form, Some("entity"), "addressCapture.contact.heading.isUK")

    val view: Document = createView(UkAddressForm.form())

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(messages("ukAddress.title", "entity"))
    }

    "display header" in {
      view.select("h1").text() must include(messages("ukAddress.title", "entity"))
    }

    "display radios" in {
      view must containElementWithID("ukAddress")
      view must containElementWithID("ukAddress-2")
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    "display error if question not answered" in {
      val form = UkAddressForm
        .form()
        .bind(Map("ukAddress" -> ""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(UkAddressForm.form(), Some("entity"), "addressCapture.business.heading.isUK")(registrationJourneyRequest, messages)
    page.render(UkAddressForm.form(), Some("entity"), "addressCapture.business.heading.isUK", registrationJourneyRequest, messages)
  }

}
