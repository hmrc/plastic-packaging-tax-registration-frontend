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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import forms.YesNoValues
import forms.liability.{ExpectToExceedThresholdWeight, ExpectToExceedThresholdWeightAnswer}
import views.html.liability.expect_to_exceed_threshold_weight_page

import java.time.LocalDate

class ExpectToExceedThresholdWeightViewSpec
  extends UnitViewSpec with Matchers with TableDrivenPropertyChecks {

  private val page = inject[expect_to_exceed_threshold_weight_page]
  private val formProvider: ExpectToExceedThresholdWeight = inject[ExpectToExceedThresholdWeight]

  private def createView(form: Form[ExpectToExceedThresholdWeightAnswer] = formProvider()): Document =
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

      view must containElementWithID("value-yes")
      view.getElementById("value-yes").attr("value") mustBe YesNoValues.YES
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

      val form = formProvider().fill(ExpectToExceedThresholdWeightAnswer(true, Some(LocalDate.now())))
      val view = createView(form)

      view.getElementById("value-yes").attr("value") mustBe "yes"
    }

    "display error" when {

      "no radio button checked" in {

        val form = formProvider()
          .bind(emptyFormData)
        val view = createView(form)

        view must haveGovukFieldError("answer", messages(formProvider.emptyError))
        view must haveGovukGlobalErrorSummary
      }
    }


    "Expect to Exceed Threshold Date" should {

      val view = createView()

      "contain timeout dialog function" in {
        containTimeoutDialogFunction(view) mustBe true
      }


      "display section header" in {
        view.select("span#section-header").text() must include(messages("liability.sectionHeader"))
      }

      "display question" in {
        view.select("#conditional-value-yes > div > fieldset > legend").text() must include(
          messages("liability.expectToExceedThreshold.date.question")
        )
      }

      "display question hint" in {
        view.getElementById("expect-to-exceed-threshold-weight-date-hint") must containMessage(
          "liability.expectToExceedThreshold.date.hint"
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

      "display data in date inputs" in {
        val form = formProvider()
          .fill(ExpectToExceedThresholdWeightAnswer(true, Some(LocalDate.of(2022, 5, 1))))
        val view = createView(form)

        view.getElementById("expect-to-exceed-threshold-weight-date.day").attr("value") mustBe "1"
        view.getElementById("expect-to-exceed-threshold-weight-date.month").attr("value") mustBe "5"
        view.getElementById("expect-to-exceed-threshold-weight-date.year").attr("value") mustBe "2022"
      }

      "display error" when {
        "no date entered" in {
          val boundForm = formProvider().withError("answerError", "general.true")
          val view = createView(boundForm)
          view must haveGovukFieldError("expect-to-exceed-threshold-weight-date", "Yes")
          view must haveGovukGlobalErrorSummary
        }

        val day = Some("expect-to-exceed-threshold-weight-date.day" -> "5")
        val month = Some("expect-to-exceed-threshold-weight-date.month" -> "12")
        val year = Some("expect-to-exceed-threshold-weight-date.year" -> "2022")

        val table = Table(
          ("test", "day", "month", "year"),
          ("day", None, month, year),
          ("month",day, None, year),
          ("year",day, month, None),
          ("day and year", None, month, None),
          ("day and month", None, None, year),
          ("month and year", day, None, None),
        )

        forAll(table) {
          (
            test: String,
           day: Option[(String, String)],
           month: Option[(String, String)],
           year: Option[(String, String)]
          ) =>
            s"$test is missing" in {
              val boundForm = formProvider().bind(
                Map("answer" -> "yes") ++
                  day.fold[Map[String,String]](Map())(o => Map(o._1 -> o._2)) ++
                  month.fold[Map[String,String]](Map())(o => Map(o._1 -> o._2)) ++
                  year.fold[Map[String,String]](Map())(o => Map(o._1 -> o._2))
              )

              createView(boundForm)
                .getElementsByClass("govuk-error-message")
                .text() must include(s"Date must include the $test")
            }
        }

      }

    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(formProvider())(request, messages)
    page.render(formProvider(), request, messages)
  }

}
