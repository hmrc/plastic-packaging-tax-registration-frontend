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
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.data.Form
import play.api.i18n.Messages
import forms.YesNoValues
import forms.liability.{ExceededThresholdWeight, ExceededThresholdWeightAnswer}
import views.html.liability.exceeded_threshold_weight_page

import java.time.{Clock, Instant}
import java.util.TimeZone

class ExceededThresholdWeightViewSpec extends UnitViewSpec with Matchers with TableDrivenPropertyChecks {

  val mockMessages: Messages = mock[Messages]
  when(mockMessages.apply(anyString(), any())).thenReturn("some message")

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  private val formProvider = new ExceededThresholdWeight(appConfig, fakeClock)

  val form =  formProvider.form()(mockMessages)

  private val page = inject[exceeded_threshold_weight_page]

  private def createView(form: Form[ExceededThresholdWeightAnswer] = form, backLookChangeEnabled: Boolean = false): Document =
    page(form, backLookChangeEnabled)(journeyRequest, messages)

  "The view" should {
    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "have a 'Back' button" in {
      view.getElementById("back-link").text must not be null
    }

    "display title" when {
      "backLookChangeEnabled is true" in {
        val title = createView(backLookChangeEnabled = true).select("title").text()
        title mustBe "Have you manufactured or imported 10,000kg or more of finished plastic packaging in the last 12 months? - Register for Plastic Packaging Tax - GOV.UK"
        title must include(messages("liability.exceededThresholdWeight.title"))
      }

      //todo: remove post april
      "backLookChangeEnabled is false" in {
        val title = createView().select("title").text()
        title mustBe "Have you manufactured or imported 10,000kg or more finished plastic packaging since 1 April 2022? - Register for Plastic Packaging Tax - GOV.UK"
        title must include(messages("liability.exceededThresholdWeight.before.april.2023.title"))
      }
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

    "display question" when {
      "backLookChangeEnabled is true" in {
        val question = createView(backLookChangeEnabled = true).getElementsByClass("govuk-heading-l").text()
        question mustBe "Have you manufactured or imported 10,000kg or more of finished plastic packaging in the last 12 months?"
        question mustBe messages("liability.exceededThresholdWeight.question")
      }

      //todo: remove post april
      "backLookChangeEnabled is false" in {
        val question = createView().getElementsByClass("govuk-heading-l").text()
        question mustBe "Have you manufactured or imported 10,000kg or more finished plastic packaging since 1 April 2022?"
        question mustBe messages("liability.exceededThresholdWeight.before.april.2023.question")
      }
    }

    "display date question" in {
      view.select("#conditional-value-yes > div > fieldset > legend").text() must include(
        messages("liability.exceededThresholdWeightDate.title")
      )
    }

    "display hint according to feature flag" when {
      "isPostAprilEnabled is true" in {
        val hint = createView(backLookChangeEnabled = true).getElementsByClass("govuk-body").text()

        hint must include("This is the total of all the plastic packaging you’ve manufactured or imported in the last 12 months.")
        hint must include(messages("liability.exceededThresholdWeight.line1"))
      }

      //todo: remove post april
      "backLookChangeEnabled is false" in {
        val hint = createView().getElementsByClass("govuk-body").text()

        hint must include("This is the total of all the plastic you’ve manufactured or imported since April.")
        hint must include(messages("liability.exceededThresholdWeight.before.april.2023.line1"))
      }

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

    // TODO need a working way to test which radio is checked (or neither) when form is first displayed


    "display error" when {

      "no answer is selected" when {
        "backLookChangeEnabled is true" in {

          when(appConfig.backLookChangeEnabled).thenReturn(true)
          val form =  formProvider.form()(mockMessages)

          val errorText = createView(form.bind(Map("answer" -> "")), true)
            .getElementsByClass("govuk-error-message")
            .text()

          errorText must include("Select yes if you have manufactured or imported 10,000kg or more of finished plastic packaging in the last 12 months.")
        }

        "backLookChangeEnabled is false" in {
          when(appConfig.backLookChangeEnabled).thenReturn(false)
          val form =  formProvider.form()(mockMessages)

          val errorText = createView(form = form.bind(Map("answer" -> "")))
            .getElementsByClass("govuk-error-message")
            .text()

          errorText must include("Select yes if you have manufactured or imported 10,000kg or more finished plastic packaging since 1 April 2022")
          errorText must include(messages("liability.exceededThresholdWeight.before.april.2023.question.empty.error"))
        }
      }
      "form has error" in {
        val boundForm = form.withError("answerError","general.true")
        val view = createView(boundForm)
        view must haveGovukFieldError("exceeded-threshold-weight-date", "Yes")
        view must haveGovukGlobalErrorSummary
      }

      val day = Some("exceeded-threshold-weight-date.day" -> "5")
      val month = Some("exceeded-threshold-weight-date.month" -> "12")
      val year = Some("exceeded-threshold-weight-date.year" -> "2022")

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
            val boundForm = formProvider.form().bind(
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

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(form, false)(request, messages)
    page.render(form, false, request, messages)
  }

}
