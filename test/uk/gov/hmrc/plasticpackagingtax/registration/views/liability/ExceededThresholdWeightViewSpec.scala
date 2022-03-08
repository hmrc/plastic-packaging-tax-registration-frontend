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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExceededThresholdWeight
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.exceeded_threshold_weight_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ExceededThresholdWeightViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[exceeded_threshold_weight_page]

  private def createView(form: Form[Boolean] = ExceededThresholdWeight.form()): Document =
    page(form)(journeyRequest, messages)

  "The view" should {
    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(routes.StartController.displayStartPage())
    }

    "display title" in {
      view.select("title").text() must include(messages("liability.exceededThresholdWeight.title"))
    }

    "display header" in {
      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("liability.expectToExceedThresholdWeight.sectionHeader")
      )
    }

    "display radio inputs" in {
      view must containElementWithID("answer")
      view.getElementsByClass("govuk-label").first().text() mustBe "Yes"
      view must containElementWithID("answer-2")
      view.getElementsByClass("govuk-label").get(1).text() mustBe "No"
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    // TODO need a working way to test which radio is checked (or neither) when form is first displayed

    "display error" when {
      "no radio button checked" in {
        val form = ExceededThresholdWeight.form().bind(emptyFormData)
        val view = createView(form)
        view must haveGovukFieldError("answer", messages(ExceededThresholdWeight.emptyError))
        view must haveGovukGlobalErrorSummary
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(ExceededThresholdWeight.form())(request, messages)
    page.render(ExceededThresholdWeight.form(), request, messages)
  }

}
