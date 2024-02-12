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

package views.liability

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import forms.liability.LiabilityWeight
import views.components.Styles
import views.html.liability.liability_weight_page

class LiabilityWeightViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[liability_weight_page]

  private def createView(form: Form[LiabilityWeight] = LiabilityWeight.form()): Document =
    page(form)(registrationJourneyRequest, messages)

  "Liability Weight View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("liabilityWeightPage.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("liability.sectionHeader")
      )
    }

    "display liability weight question" in {
      view.getElementsByClass(Styles.gdsLabelPageHeading).text() must include(
        messages("liabilityWeightPage.question")
      )
    }

    "display hint" in {
      view.getElementsByClass("govuk-hint").text() must include(
        "Enter the weight, in kilograms. 1 tonne is 1,000kg.")

      view.getElementsByClass("govuk-hint").text() must include(
        messages("liabilityWeightPage.hint")
      )
    }

    "display total weight input box" in {
      view must containElementWithID("totalKg")
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Liability Weight View when filled" should {

    "display data in weight input" in {

      val form = LiabilityWeight
        .form()
        .fill(LiabilityWeight(Some(1000)))
      val view = createView(form)

      view.getElementById("totalKg").attr("value") mustBe "1000"
    }
  }

  "display error" when {

    "weight is not entered" in {

      val form = LiabilityWeight
        .form()
        .bind(Map("totalKg" -> ""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("totalKg", "Enter the weight, in kilograms")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(LiabilityWeight.form())(request, messages)
    page.render(LiabilityWeight.form(), request, messages)
  }

}
