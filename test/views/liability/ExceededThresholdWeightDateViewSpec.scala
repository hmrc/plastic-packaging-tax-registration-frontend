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

import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{FormWithCSRF, GovukDateInput, GovukRadios}
import views.html.components._
import views.html.liability.{exceeded_threshold_weight_date_page, exceeded_threshold_weight_page}
import views.html.main_template
import views.viewmodels.govuk.radios._
import views.viewmodels.{BackButtonJs, Title}

class ExceededThresholdWeightDateViewSpec extends PlaySpec with BeforeAndAfterEach {

  val request = FakeRequest()
  val mockMessages: Messages = mock[Messages]

  val form: Form[Boolean] = Form[Boolean]("value" -> ignored[Boolean](true))
  val sectionHeader: sectionHeader = mock[sectionHeader]
  val pageHeading: pageHeading = mock[pageHeading]
  val govUkLayout: main_template = mock[main_template]
  val contentCaptor = ArgumentCaptor.forClass(classOf[Html])
  val saveButtons = mock[saveButtons]
  val errorSummary = mock[errorSummary]
  val govukDateInput = mock[GovukDateInput]
  val paragraphBody = mock[paragraphBody]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMessages, sectionHeader, pageHeading, govUkLayout, saveButtons, errorSummary, govukDateInput, paragraphBody)

    when(mockMessages.apply(anyString(), any())).thenReturn("some message") //todo?
    when(sectionHeader.apply(any())).thenReturn(HtmlFormat.raw("SECTION HEADER"))
    when(pageHeading.apply(any(), any(), any())).thenReturn(HtmlFormat.raw("PAGE HEADING"))
    when(govUkLayout.apply(any(), any(), any())(contentCaptor.capture())(any(), any())).thenReturn(HtmlFormat.raw("GOVUK"))
    when(saveButtons.apply(any())(any())).thenReturn(HtmlFormat.raw("SAVE BUTTONS"))
    when(errorSummary.apply(any(), any())(any())).thenReturn(HtmlFormat.raw("ERROR SUMMARY"))
    when(govukDateInput.apply(any())).thenReturn(HtmlFormat.raw("GOV UK DATE INPUT"))
    when(paragraphBody.apply(any(), any(), any())).thenReturn(HtmlFormat.raw("PARAGRAPH 0"), Seq(1, 2, 3).map(i => HtmlFormat.raw(s"PARAGRAPH $i")):_*)
  }

  private val page = new exceeded_threshold_weight_date_page(
    formHelper = new FormWithCSRF,
    sectionHeader = sectionHeader,
    pageHeading = pageHeading,
    govukLayout = govUkLayout,
    saveButtons = saveButtons,
    errorSummary = errorSummary,
    paragraphBody = paragraphBody,
    govukDateInput = govukDateInput
  )

  "view" must {
    "use govUk layout" in {
      instantiateView()

      verify(govUkLayout).apply(
        meq(Title("liability.exceededThresholdWeight.title")),
        meq(Some(BackButtonJs())),
        any())(any())(meq(request), meq(mockMessages))
    }

    "have the form" in {
      instantiateView()

      val form = Jsoup.parse(insideGovUkWrapper).getElementsByTag("form").first()

      form.attr("method") mustBe controllers.liability.routes.ExceededThresholdWeightDateController.submit().method
      form.attr("action") mustBe controllers.liability.routes.ExceededThresholdWeightDateController.submit().url
      form.attr("autoComplete") mustBe "off"
      assert(form.hasAttr("novalidate"))
    }

    "have the error summary" in {
      instantiateView()

      insideGovUkWrapper must include("ERROR SUMMARY")
      verify(errorSummary).apply(form.errors)(mockMessages)
    }

    "have the section header" in {
      instantiateView()

      insideGovUkWrapper must include("SECTION HEADER")
      verify(sectionHeader).apply("some message")
      verify(mockMessages).apply("liability.sectionHeader")
    }

    "have the h1" in {
      instantiateView()

      insideGovUkWrapper must include("PAGE HEADING")
      verify(pageHeading).apply("some message")
      verify(mockMessages).apply("liability.exceededThresholdWeight.question")
    }

    "have the radio buttons" in {
      instantiateView()

      insideGovUkWrapper must include("GOV UK DATE INPUT")
      verify(govukDateInput).apply(any())
    }

    "have the continue button" in {
      instantiateView()

      insideGovUkWrapper must include("SAVE BUTTONS")
      verify(saveButtons).apply()(mockMessages)
    }
  }

  //todo this is $h!t
  "Exercise generated rendering methods" in {
    page.f(form)(request, mockMessages)
    page.render(form, request, mockMessages)
  }

  def instantiateView(): HtmlFormat.Appendable = page(form)(request, mockMessages)
  def insideGovUkWrapper = contentCaptor.getValue.toString

}
