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
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.YesNoValues
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{ExceededThresholdWeight, ExceededThresholdWeightAnswer}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.exceeded_threshold_weight_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

import java.time.{Clock, Instant}
import java.util.TimeZone

@ViewTest
class ExceededThresholdWeightViewSpec extends UnitViewSpec with Matchers {

  val mockMessages: Messages = mock[Messages]
  when(mockMessages.apply(anyString(), any())).thenReturn("some message")

  private val mockAppConfig = mock[AppConfig]
  private val fakeClock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  private val formProvider = new ExceededThresholdWeight(appConfig, fakeClock)

  val form =  formProvider.form()(mockMessages)

  private val page = inject[exceeded_threshold_weight_page]

  private def createView(form: Form[ExceededThresholdWeightAnswer] = form): Document =
    page(form)(journeyRequest, messages)

  "The view" should {
    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "should not have a 'Back' button" in {
      view.getElementById("back-link") mustBe null
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
      view must containElementWithID("value-yes")
      view.getElementById("value-yes").attr("value") mustBe YesNoValues.YES
      view must containElementWithID("value-no")
      view.getElementById("value-no").attr("value") mustBe YesNoValues.NO

    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

    // TODO need a working way to test which radio is checked (or neither) when form is first displayed

    "display error" when {
      "when form has error" in {
        val bindedForm = form.withError("answerError","general.true")
        val view = createView(bindedForm)
        view must haveGovukFieldError("exceeded-threshold-weight-date", "Yes")
        view must haveGovukGlobalErrorSummary
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(form)(request, messages)
    page.render(form, request, messages)
  }

}
