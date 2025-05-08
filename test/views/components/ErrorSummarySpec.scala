/*
 * Copyright 2025 HM Revenue & Customs
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

package views.components

import base.unit.UnitViewSpec
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError
import views.html.components.errorSummary

class ErrorSummarySpec extends UnitViewSpec with Matchers {

  override def beforeEach(): Unit =
    super.beforeEach()

  val sut: errorSummary = inject[errorSummary]

  val errors = Seq(
    FormError(
      "liability.exceededThresholdWeight.question.empty.error ",
      "liability.exceededThresholdWeight.question.empty.error"
    )
  )

  "apply" must {
    "display nothing" when {
      "there are no errors" in {
        val html         = sut.apply(Seq.empty)(messages)
        val errorSummary = Jsoup.parse(html.toString).body().children()

        assert(errorSummary.isEmpty)
      }
    }

    "display the govUkErrorSummary" when {

      "using the form error key" in {

        val html                   = sut.apply(errors)(messages)
        val errorSummary: Elements = Jsoup.parse(html.toString).body().children()

        errorSummary.text() mustBe "There is a problem Select yes if you have met the threshold in the last 12 months"
        errorSummary.html().contains("There is a problem") mustBe true
        errorSummary.html().contains("Select yes if you have met the threshold in the last 12 months") mustBe true
        errorSummary.html().contains("govuk-list govuk-error-summary__list") mustBe true
      }

      "using the override error key" in {
        val html         = sut.apply(errors, Some("override-key"))(messages)
        val errorSummary = Jsoup.parse(html.toString).body().children()

        errorSummary.text() mustBe "There is a problem Select yes if you have met the threshold in the last 12 months"
        errorSummary.html().contains("#override-key") mustBe true

      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    sut.f(errors, None)
    sut.render(errors, None, messages)
  }

}
