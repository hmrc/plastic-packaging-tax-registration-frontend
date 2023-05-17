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

package views.partner

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import forms.enrolment.IsUkAddress.{NO, YES}
import forms.partner.RemovePartner
import views.html.partner.remove_partner_page
import java.util.UUID


class RemovePartnerViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[remove_partner_page]

  private val partnerName = "Plastic Packaging Partner"
  private val partnerId   = UUID.randomUUID.toString

  private def createView(form: Form[RemovePartner] = RemovePartner.form()): Document =
    page(form, partnerName, partnerId)(registrationJourneyRequest, messages)

  "Remove Partner View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(
        messages("partnership.removePartner.title", partnerName)
      )
    }

    "display heading question" in {
      view.select("h1").text() must include(
        messages("partnership.removePartner.title", partnerName)
      )
    }

    "display radio inputs" in {
      view.getElementById("value").attr("value").text() mustBe YES
      view.getElementsByClass("govuk-label").get(0).text() mustBe messages("general.true")

      view.getElementById("value-2").attr("value").text() mustBe NO
      view.getElementsByClass("govuk-label").get(1).text() mustBe messages("general.false")
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    "display error" when {

      "no radio button checked" in {
        val form = RemovePartner
          .form()
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("value", messages("partnership.removePartner.error.empty"))
        view must haveGovukGlobalErrorSummary
      }

    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(RemovePartner.form(), partnerName, partnerId)(registrationJourneyRequest, messages)
    page.render(RemovePartner.form(), partnerName, partnerId, registrationJourneyRequest, messages)
  }

}
