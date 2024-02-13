/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.refEq
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.i18n.Messages
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.Aliases.Legend
import uk.gov.hmrc.govukfrontend.views.html.components.{FormWithCSRF, GovukRadios}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import views.html.components._
import views.html.liability.exceeded_threshold_weight_page
import views.html.main_template
import views.viewmodels.govuk.radios._
import views.viewmodels.{BackButtonJs, Title}

class ExceededThresholdWeightViewSpec extends PlaySpec with BeforeAndAfterEach with MockitoSugar with ResetMocksAfterEachTest {

  private val request      = FakeRequest()
  private val mockMessages = mock[Messages]

  private val form          = Form[Boolean]("value" -> ignored[Boolean](true))
  private val sectionHeader = mock[sectionHeader]
  private val pageHeading   = mock[pageHeading]
  private val govUkLayout   = mock[main_template]
  private val contentCaptor = ArgCaptor[Html]
  private val saveButtons   = mock[saveButtons]
  private val errorSummary  = mock[errorSummary]
  private val govukRadios   = mock[GovukRadios]
  private val inset         = mock[inset]
  private val paragraphBody = mock[paragraphBody]
  private val link          = mock[link]

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockMessages.apply(any[String], any)).thenReturn("some message")
    when(sectionHeader.apply(any)).thenReturn(HtmlFormat.raw("SECTION HEADER"))
    when(pageHeading.apply(any, any, any)).thenReturn(HtmlFormat.raw("PAGE HEADING"))
    when(govUkLayout.apply(any, any, any)(contentCaptor)(any, any)).thenReturn(HtmlFormat.raw("GOVUK"))
    when(saveButtons.apply(any)(any)).thenReturn(HtmlFormat.raw("SAVE BUTTONS"))
    when(errorSummary.apply(any, any)(any)).thenReturn(HtmlFormat.raw("ERROR SUMMARY"))
    when(govukRadios.apply(any)).thenReturn(HtmlFormat.raw("GOV UK RADIOS"))
    when(paragraphBody.apply(any, any, any)).thenReturn(HtmlFormat.raw("PARAGRAPH 0"), Seq(1, 2, 3).map(i => HtmlFormat.raw(s"PARAGRAPH $i")): _*)
  }

  private val page =
    new exceeded_threshold_weight_page(new FormWithCSRF, sectionHeader, pageHeading, govUkLayout, saveButtons, errorSummary, govukRadios, inset, paragraphBody, link)

  "view" must {
    "use govUk layout" in {
      instantiateView()

      verify(govUkLayout).apply(refEq(Title(form, "liability.exceededThresholdWeight.title")), eqTo(Some(BackButtonJs)), any)(any)(eqTo(request), eqTo(mockMessages))
    }

    "have the form" in {
      instantiateView()

      val form = Jsoup.parse(insideGovUkWrapper).getElementsByTag("form").first()
      form.attr("method") mustBe controllers.liability.routes.ExceededThresholdWeightController.submit.method
      form.attr("action") mustBe controllers.liability.routes.ExceededThresholdWeightController.submit.url
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
      verify(mockMessages).apply("liability.exceededThresholdWeight.line1")
      verify(mockMessages).apply("liability.exceededThresholdWeight.inset")
      verify(mockMessages).apply("liability.exceededThresholdWeight.line2")
      verify(paragraphBody, times(4)).apply("some message") // including inset and link-to-guidance
    }

    "have link to guidance" in {
      val linkToGuidance = mock[Html]
      when(link.apply(any, any, any, any, any, any)) thenReturn linkToGuidance
      instantiateView()
      verify(link).apply("some message", Call("GET", "https://www.gov.uk/guidance/when-you-must-register-for-plastic-packaging-tax#when-to-register"))
      verify(mockMessages).apply("liability.exceededThresholdWeight.line4", linkToGuidance)
      verify(mockMessages).apply("liability.exceededThresholdWeight.line4.link-text")
    }

    "have the inset" in {
      val paragraph = mock[Html]
      when(paragraphBody.apply(any, any, any)) thenReturn paragraph
      instantiateView()
      verify(inset).apply(paragraph)
      verify(mockMessages).apply("liability.exceededThresholdWeight.inset")
    }

    "have the radio buttons" in {
      instantiateView()

      insideGovUkWrapper must include("GOV UK RADIOS")
      verify(govukRadios).apply(
        RadiosViewModel.yesNo(
          field = form("value"),
          legend = Legend(content = Text("some message"), classes = "govuk-fieldset__legend govuk-fieldset__legend govuk-fieldset__legend--m", isPageHeading = false)
        )(mockMessages).inline()
          .withHint(Hint(content = Text("some message")))
      )

      verify(mockMessages).apply("liability.exceededThresholdWeight.question")
    }

    "have the continue button" in {
      instantiateView()

      insideGovUkWrapper must include("SAVE BUTTONS")
      verify(saveButtons).apply()(mockMessages)
    }
  }

  "Exercise generated rendering methods" in {
    page.f(form)(request, mockMessages)
    page.render(form, request, mockMessages)
  }

  def instantiateView(): HtmlFormat.Appendable = page(form)(request, mockMessages)
  def insideGovUkWrapper                       = contentCaptor.value.toString

}
