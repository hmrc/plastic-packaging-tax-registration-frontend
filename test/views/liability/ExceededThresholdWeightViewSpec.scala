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
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{FormWithCSRF, GovukRadios}
import views.html.components.{errorSummary, pageHeading, paragraphBody, saveButtons, sectionHeader}
import views.html.liability.exceeded_threshold_weight_page
import views.html.main_template
import views.viewmodels.govuk.radios._
import views.viewmodels.{BackButtonJs, Title}
import org.mockito.MockitoSugar.{mock, reset, times, verify, when}
import org.mockito.captor.ArgCaptor

class ExceededThresholdWeightViewSpec extends PlaySpec with BeforeAndAfterEach {

  private val request = FakeRequest()
  private val mockMessages = mock[Messages]

  private val form = Form[Boolean]("value" -> ignored[Boolean](true))
  private val sectionHeader = mock[sectionHeader]
  private val pageHeading = mock[pageHeading]
  private val govUkLayout = mock[main_template]
  private val contentCaptor = ArgCaptor[Html]
  private val saveButtons = mock[saveButtons]
  private val errorSummary = mock[errorSummary]
  private val govukRadios = mock[GovukRadios]
  private val paragraphBody = mock[paragraphBody]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMessages, sectionHeader, pageHeading, govUkLayout, saveButtons, errorSummary, govukRadios, paragraphBody)

    when(mockMessages.apply(any[String], any)).thenReturn("some message") //todo?
    when(sectionHeader.apply(any)).thenReturn(HtmlFormat.raw("SECTION HEADER"))
    when(pageHeading.apply(any, any, any)).thenReturn(HtmlFormat.raw("PAGE HEADING"))
    when(govUkLayout.apply(any, any, any)(contentCaptor)(any, any)).thenReturn(HtmlFormat.raw("GOVUK"))
    when(saveButtons.apply(any)(any)).thenReturn(HtmlFormat.raw("SAVE BUTTONS"))
    when(errorSummary.apply(any, any)(any)).thenReturn(HtmlFormat.raw("ERROR SUMMARY"))
    when(govukRadios.apply(any)).thenReturn(HtmlFormat.raw("GOV UK RADIOS"))
    when(paragraphBody.apply(any, any, any)).thenReturn(HtmlFormat.raw("PARAGRAPH 0"), Seq(1, 2, 3).map(i => HtmlFormat.raw(s"PARAGRAPH $i")):_*)
  }

  private val page = new exceeded_threshold_weight_page(
    new FormWithCSRF,
    sectionHeader,
    pageHeading,
    govUkLayout,
    saveButtons,
    errorSummary,
    govukRadios,
    paragraphBody
  )

  "view" must {
    "use govUk layout" in {
      instantiateView()

      verify(govUkLayout).apply(
        eqTo(Title("liability.exceededThresholdWeight.title")),
        eqTo(Some(BackButtonJs())),
        any)(any)(eqTo(request), eqTo(mockMessages))
    }

    "have the form" in {
      instantiateView()

      val form = Jsoup.parse(insideGovUkWrapper).getElementsByTag("form").first()
      form.attr("method") mustBe controllers.liability.routes.ExceededThresholdWeightController.submit().method
      form.attr("action") mustBe controllers.liability.routes.ExceededThresholdWeightController.submit().url
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

    "have the paragraphs" in {
      instantiateView()

      insideGovUkWrapper must include("PARAGRAPH 0")
      insideGovUkWrapper must include("PARAGRAPH 1")
      insideGovUkWrapper must include("PARAGRAPH 2")
      verify(paragraphBody, times(3)).apply("some message")
      verify(mockMessages).apply("liability.exceededThresholdWeight.line1")
      verify(mockMessages).apply("liability.exceededThresholdWeight.line2")
      verify(mockMessages).apply("liability.exceededThresholdWeight.line3")
    }

    "have the radio buttons" ignore { // TODO fix
      instantiateView()

      insideGovUkWrapper must include("GOV UK RADIOS")
      verify(govukRadios).apply(RadiosViewModel.yesNo(field = form("value"))(mockMessages))
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
  def insideGovUkWrapper = contentCaptor.value.toString

}
