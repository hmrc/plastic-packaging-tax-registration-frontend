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

package uk.gov.hmrc.plasticpackagingtax.registration.views.liability

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityExpectedWeight
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.liability_weight_expected_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class LiabilityExpectedWeightViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[liability_weight_expected_page]

  private def createView(
    form: Form[LiabilityExpectedWeight] = LiabilityExpectedWeight.form()
  ): Document =
    page(form)(journeyRequest, messages)

  "Liability Weight Expected View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") mustBe null
    }

    "display title" in {

      view.select("title").text() must include(messages("liabilityExpectedWeightPage.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("liabilityExpectedWeightPage.sectionHeader")
      )
    }

    "display liability weight question" in {

      view.getElementsByClass("govuk-fieldset__heading").text() must include(
        messages("liabilityExpectedWeightPage.question")
      )
    }

    "display total weight label" in {

      view.getElementsByAttributeValue("for", "totalKg").get(0).text() mustBe messages(
        "liabilityExpectedWeightPage.label"
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

      val form = LiabilityExpectedWeight
        .form()
        .fill(aRegistration().liabilityDetails.expectedWeight.get)
      val view = createView(form)

      view.getElementById("totalKg").attr("value") mustBe "12000"
    }
  }

  "display error" when {

    "weight is not entered" in {

      val form = LiabilityExpectedWeight
        .form()
        .bind(Map("answer" -> "yes", "totalKg" -> ""))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("totalKg", "Enter the expected weight, in kilograms")
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(LiabilityExpectedWeight.form())(request, messages)
    page.render(LiabilityExpectedWeight.form(), request, messages)
  }

}
