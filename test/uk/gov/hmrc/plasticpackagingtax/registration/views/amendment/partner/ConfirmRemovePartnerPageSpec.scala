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

package uk.gov.hmrc.plasticpackagingtax.registration.views.amendment.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.forms.partner.RemovePartner
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.partner.confirm_remove_partner_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ConfirmRemovePartnerPageSpec extends UnitViewSpec with Matchers {

  private val page = inject[confirm_remove_partner_page]

  private val partner = aLimitedCompanyPartner()

  "Confirm Remove Partner page" should {

    val view: Document = page(RemovePartner.form(), partner)(journeyRequest, messages)

    "contain title" in {
      view.select("title").text() must include(
        messages(key = "amend.partner.remove.title", args = partner.name)
      )
    }

    "contain heading" in {
      view.select("h1").text() mustBe messages(key = "amend.partner.remove.title",
                                               args = partner.name
      )
    }

    "contain radio groups with yes option" in {
      view must containElementWithID("value")
      val yesRadio = view.getElementById("value")
      yesRadio.attr("type") mustBe "radio"
      yesRadio.attr("value") mustBe "yes"
    }

    "display 'Continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Continue"
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(RemovePartner.form(), partner)(journeyRequest, messages)
    page.render(RemovePartner.form(), partner, journeyRequest, messages)
  }

}
