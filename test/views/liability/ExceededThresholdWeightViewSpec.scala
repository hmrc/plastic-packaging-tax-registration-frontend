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
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.i18n.Messages
import views.html.liability.exceeded_threshold_weight_page

class ExceededThresholdWeightViewSpec extends UnitViewSpec with Matchers with TableDrivenPropertyChecks {

  val mockMessages: Messages = mock[Messages]
  when(mockMessages.apply(anyString(), any())).thenReturn("some message")

  val form: Form[Boolean] = Form[Boolean]("answer" -> ignored[Boolean](true))

  private val page = inject[exceeded_threshold_weight_page]

  "The view" should {
    val view: Document = page(form)

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "have a 'Back' button" in {
      view.getElementById("back-link").text must not be null
    }

    "display title" in {
      val title = view.select("title").text()
      title mustBe "Have you manufactured or imported 10,000kg or more of finished plastic packaging in the last 12 months? - Register for Plastic Packaging Tax - GOV.UK"
      title must include(messages("liability.exceededThresholdWeight.title"))
    }

    "display header" in {
      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("liability.expectToExceedThresholdWeight.sectionHeader")
      )
    }

    "display radio inputs" in {
      view must containElementWithID("value-yes")
      view.getElementById("value-yes").attr("value") mustBe YesNoValues.YES
      view must containElementWithID("value-no")
      view.getElementById("value-no").attr("value") mustBe YesNoValues.NO

    }

    "display question" in {
      val question = view.getElementsByClass("govuk-heading-l").text()
      question mustBe "Have you manufactured or imported 10,000kg or more of finished plastic packaging in the last 12 months?"
      question mustBe messages("liability.exceededThresholdWeight.question")
    }

    "display date question" in {
      view.select("#conditional-value-yes > div > fieldset > legend").text() must include(
        messages("liability.exceededThresholdWeightDate.title")
      )
    }

    "display hint" in {
      val hint = view.getElementsByClass("govuk-body").text()

      hint must include("This is the total of all the plastic packaging you’ve manufactured or imported in the last 12 months.")
      hint must include(messages("liability.exceededThresholdWeight.line1"))
    }

    "display question hint" in {
      val hint = view.getElementsByClass("govuk-body").text()

      hint must include("For example, you manufactured 5,000kg in April, 2,000kg in May and 3,000kg in June.")
      hint must include(messages("liability.exceededThresholdWeight.line2"))

      hint must include("f you’re registering as a group, each member must have met this threshold.")
      hint must include(messages("liability.exceededThresholdWeight.line3"))
    }

    "display date question hint" in {
      view.getElementById("exceeded-threshold-weight-date-hint") must containMessage(
        "liability.exceededThresholdWeightDate.hint"
      )
    }

    "display day input box" in {
      view.getElementsByAttributeValueMatching("for", "day").text() must include(
        messages("date.day")
      )
    }

    "display month input box" in {
      view.getElementsByAttributeValueMatching("for", "month").text() must include(
        messages("date.month")
      )
    }

    "display year input box" in {
      view.getElementsByAttributeValueMatching("for", "year").text() must include(
        messages("date.year")
      )
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    "form action should redirect to ExceededThreshold for post April 2023" in {
      view.select("form").attr("method") mustBe "POST"
      view.select("form").attr("action") mustBe
        controllers.liability.routes.ExceededThresholdWeightController.submit().url
    }

    "display error" when {

      "no answer is selected" in {
          val errorText = page(form.withError("answer", "liability.exceededThresholdWeight.question.empty.error"))
            .getElementsByClass("govuk-error-message")
            .text()

          errorText must include("Select yes if you have manufactured or imported 10,000kg or more of finished plastic packaging in the last 12 months")
        }
      }

      "form has error" in {
        val errorForm = form.withError("answerError","general.true")
        val errorView: Document = page(errorForm)
        errorView must haveGovukFieldError("exceeded-threshold-weight-date", "Yes")
        errorView must haveGovukGlobalErrorSummary
      }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(form)(request, messages)
    page.render(form, request, messages)
  }

}
