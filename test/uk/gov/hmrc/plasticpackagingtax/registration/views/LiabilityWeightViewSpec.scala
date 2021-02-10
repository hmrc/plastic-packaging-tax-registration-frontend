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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_weight_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class LiabilityWeightViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[liability_weight_page]

  private def createView(form: Form[LiabilityWeight] = LiabilityWeight.form()): Document =
    page(form)(request, messages)

  "Liability Weight View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("liabilityWeightPage.title")
      messages must haveTranslationFor("liabilityWeightPage.question")
      messages must haveTranslationFor("liabilityWeightPage.hint")
      messages must haveTranslationFor("liabilityWeightPage.sectionHeader")
      messages must haveTranslationFor("liabilityWeight.empty.error")
      messages must haveTranslationFor("liabilityWeight.outOfRange.error")
    }

    val view = createView()

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        routes.LiabilityStartDateController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("liabilityWeightPage.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-xl").text() must include(
        messages("liabilityWeightPage.sectionHeader")
      )
    }

    "display liability weight question" in {

      view.getElementsByAttributeValueMatching("for", "totalKg-id").text() must include(
        messages("liabilityWeightPage.question")
      )
    }

    "display question hint" in {

      view.getElementById("totalKg-id-hint") must containMessage("liabilityWeightPage.hint")
    }

    "display total weight input box" in {

      view must containElementWithID("totalKg-id")
    }

    "display 'Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and Continue"
    }
  }

  "Liability Weight View when filled" should {

    "display data in weight input" in {

      val form = LiabilityWeight
        .form()
        .fill(aRegistration().liabilityDetails.weight.get)
      val view = createView(form)

      view.getElementById("totalKg-id").attr("value") mustBe "1000"
    }
  }

  "display error" when {

    "weight is not entered" in {

      val form = LiabilityWeight
        .form()
        .fillAndValidate(LiabilityWeight(None))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("totalKg-id", "Weight cannot be empty")
    }
  }
}
