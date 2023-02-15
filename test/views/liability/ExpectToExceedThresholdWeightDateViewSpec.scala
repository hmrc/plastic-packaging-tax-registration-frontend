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

package views.liability

import base.unit.UnitViewSpec
import forms.liability.ExpectToExceedThresholdWeightDate
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import controllers.liability.{routes => pptRoutes}
import play.api.data.Form
import views.html.liability.expect_to_exceed_threshold_weight_date_page

class ExpectToExceedThresholdWeightDateViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[expect_to_exceed_threshold_weight_date_page]
  private val formProvider: ExpectToExceedThresholdWeightDate = inject[ExpectToExceedThresholdWeightDate]
  private val form = formProvider()

  private def createView(form: Form[_] = form): Document = {
    page(form)(journeyRequest, messages)
  }

  "view" should {

    "contain a title" in {
      val title = createView().select("title").text()

      title mustBe "What date did you have reasonable grounds to believe this? - Register for Plastic Packaging Tax - GOV.UK"
      title must include(messages("liability.expectToExceedThreshold.date.question"))
    }

    "contain a header" in {
      val headerText = createView().select("h1").text()

      headerText mustBe "What date did you have reasonable grounds to believe this?"
      headerText mustBe messages("liability.expectToExceedThreshold.date.question")
    }

    "contain a hint" in {
      val hintText = createView().getElementsByClass("govuk-hint").text()

      hintText mustBe "For example, 15 6 2022."
      hintText mustBe messages("liability.expectToExceedThreshold.date.hint")
    }

    "contain 'Save and continue' button" in {
      val buttonText = createView().getElementById("submit").text()

      buttonText mustBe "Save and continue"
      buttonText mustBe messages("site.button.saveAndContinue")
    }

    "have a submit action" in {
      createView().select("form").attr("action") mustBe
        pptRoutes.ExpectToExceedThresholdWeightDateController.submit.url
    }

    "display error message" when {
      "date not entered" in {
        val message = "Enter the date you had reasonable grounds to believe you would meet the 10,000kg threshold within 30 days"
        val view = createView(form.withError("expect-to-exceed-threshold-weight-date", "liability.expectToExceedThreshold.date.none"))

        assertSummaryError(view, "expect-to-exceed-threshold-weight-date", message)
        assertErrorMessage(view, message)
      }
    }
  }

  private def assertSummaryError(view: Document, key: String, message: String): Unit = {
    val element = view.getElementsByClass("govuk-error-summary")
    val text = element.text()
    text must include("There is a problem")
    text must include(message)

    element.select("a").first() must haveHref(s"#$key")
  }

  private def assertErrorMessage(view: Document,  message: String): Unit = {
    view.getElementById("expect-to-exceed-threshold-weight-date-error").text() must include(message)
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(formProvider())(request, messages)
    page.render(formProvider(), request, messages)
  }

}
