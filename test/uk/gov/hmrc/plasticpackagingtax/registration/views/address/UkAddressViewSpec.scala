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

package uk.gov.hmrc.plasticpackagingtax.registration.views.address

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.forms.address.UkAddressForm
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.address.uk_address_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class UkAddressViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[uk_address_page]

  private val backLink = Call("GET", "/back-link")

  "UK Address View" should {

    val view: Document = page(UkAddressForm.form(), backLink, Some("entity"), "addressCapture.contact.heading.isUK")

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
      view.select("title").text() must include(messages("ukAddress.title", "entity"))
    }

    "display header" in {
      view.select("h1").text() must include(messages("ukAddress.title", "entity"))
    }

    "display visually hidden labels" in {
      view.getElementsByClass("govuk-visually-hidden").get(1).text() must include(
        messages("site.back.hiddenText")
      )
    }

    "display radios" in {
      view must containElementWithID("ukAddress")
      view must containElementWithID("ukAddress-2")
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(UkAddressForm.form(), backLink, Some("entity"), "addressCapture.business.heading.isUK")(journeyRequest, messages)
    page.render(UkAddressForm.form(), backLink, Some("entity"), "addressCapture.business.heading.isUK", journeyRequest, messages)
  }

}
