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
import forms.YesNoValues
import forms.liability.ExpectToExceedThresholdWeight
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import views.html.liability.expect_to_exceed_threshold_weight_page

class ExpectToExceedThresholdWeightViewSpec
  extends UnitViewSpec with Matchers with TableDrivenPropertyChecks {

  private val page = inject[expect_to_exceed_threshold_weight_page]
  private val formProvider: ExpectToExceedThresholdWeight = inject[ExpectToExceedThresholdWeight]

  private def createView(form: Form[Boolean] = formProvider()): Document =
    page(form)(journeyRequest, messages)

  "Liability section expect process more weight view" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "not display 'Back' button" in {
      view.getElementById("back-link") mustBe null
    }

    "display title" in {

      view.select("title").text() must include(
        messages("liability.expectToExceedThresholdWeight.title")
      )
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("liability.expectToExceedThresholdWeight.sectionHeader")
      )
    }

    "display radio inputs" in {

      view must containElementWithID("value")
      view.getElementById("value").attr("value") mustBe YesNoValues.YES
      view must containElementWithID("value-no")
      view.getElementById("value-no").attr("value") mustBe YesNoValues.NO
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Liability section 'Liable Date' view when filled" should {

    "display radio button checked" in {

      val form = formProvider().bind(Map("value" -> YesNoValues.YES))
      val view = createView(form)

      view.getElementById("value").attr("value") mustBe YesNoValues.YES
    }

    "display error" when {

      "no radio button checked" in {

        val view = createView(formProvider().bind(emptyFormData))

        view must haveGovukFieldError("value", messages(formProvider.emptyError))
        view must haveGovukGlobalErrorSummary
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(formProvider())(request, messages)
    page.render(formProvider(), request, messages)
  }

}
