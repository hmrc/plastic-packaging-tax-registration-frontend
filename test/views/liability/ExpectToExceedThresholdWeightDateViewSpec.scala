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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString, refEq, eq => meq}
import org.mockito.Mockito.atLeastOnce
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.data.Forms.ignored
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.Aliases.Legend
import uk.gov.hmrc.govukfrontend.views.html.components.{FormWithCSRF, GovukDateInput}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import views.html.components._
import views.html.liability.expect_to_exceed_threshold_weight_date_page
import views.html.main_template
import views.viewmodels.govuk.date.{DateViewModel, FluentDate}
import views.viewmodels.{BackButtonJs, Title}

class ExpectToExceedThresholdWeightDateViewSpec extends PlaySpec with BeforeAndAfterEach {

  private val request        = FakeRequest()
  private val mockMessages   = mock[Messages]
  private val form           = Form[Boolean]("value" -> ignored[Boolean](true))
  private val sectionHeader  = mock[sectionHeader]
  private val pageHeading    = mock[pageHeading]
  private val govUkLayout    = mock[main_template]
  private val contentCaptor  = ArgumentCaptor.forClass(classOf[Html])
  private val saveButtons    = mock[saveButtons]
  private val errorSummary   = mock[errorSummary]
  private val govukDateInput = mock[GovukDateInput]
  private val paragraphBody  = mock[paragraphBody]
  private val bulletList     = mock[bulletList]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMessages, sectionHeader, pageHeading, govUkLayout, saveButtons, errorSummary, govukDateInput, paragraphBody, bulletList)

    when(mockMessages.apply(anyString(), any())).thenReturn("some message")
    when(sectionHeader.apply(any())).thenReturn(HtmlFormat.raw("SECTION HEADER"))
    when(pageHeading.apply(any(), any(), any())).thenReturn(HtmlFormat.raw("PAGE HEADING"))
    when(govUkLayout.apply(any(), any(), any())(contentCaptor.capture())(any(), any())).thenReturn(HtmlFormat.raw("GOVUK"))
    when(saveButtons.apply(any())(any())).thenReturn(HtmlFormat.raw("SAVE BUTTONS"))
    when(errorSummary.apply(any(), any())(any())).thenReturn(HtmlFormat.raw("ERROR SUMMARY"))
    when(govukDateInput.apply(any())).thenReturn(HtmlFormat.raw("GOV UK DATE INPUT"))
    when(bulletList.apply(any())).thenReturn(HtmlFormat.raw("BULLET LIST"))
    when(paragraphBody.apply(any(), any(), any())).thenReturn(HtmlFormat.raw("PARAGRAPH 0"), Seq(1, 2, 3).map(i => HtmlFormat.raw(s"PARAGRAPH $i")): _*)
  }

  private val page = new expect_to_exceed_threshold_weight_date_page(
    formHelper = new FormWithCSRF,
    sectionHeader = sectionHeader,
    pageHeading = pageHeading,
    govukLayout = govUkLayout,
    saveButtons = saveButtons,
    errorSummary = errorSummary,
    paragraphBody = paragraphBody,
    govukDateInput = govukDateInput,
    bulletList = bulletList
  )

  "view" must {
    "use govUk layout" in {
      instantiateView()

      verify(govUkLayout).apply(refEq(Title("liability.expectToExceedThresholdDate.title")), meq(Some(BackButtonJs)), any())(any())(meq(request), meq(mockMessages))
    }

    "have the form" in {
      instantiateView()

      val form = Jsoup.parse(insideGovUkWrapper).getElementsByTag("form").first()

      form.attr("method") mustBe controllers.liability.routes.ExpectToExceedThresholdWeightDateController.submit.method
      form.attr("action") mustBe controllers.liability.routes.ExpectToExceedThresholdWeightDateController.submit.url
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
      verify(mockMessages).apply("liability.expectToExceedThresholdDate.question")
    }

    "have the first paragraph " in {
      instantiateView()

      insideGovUkWrapper must include("PARAGRAPH 0")
      verify(paragraphBody, atLeastOnce()).apply("some message")
      verify(mockMessages).apply("liability.expectToExceedThresholdDate.p1")
    }

    "have the bullet list" in {
      instantiateView()

      insideGovUkWrapper must include("BULLET LIST")
      verify(bulletList).apply(Seq(Html("some message"), Html("some message")))
      verify(mockMessages).apply("liability.expectToExceedThresholdDate.p1.bullet.1")
      verify(mockMessages).apply("liability.expectToExceedThresholdDate.p1.bullet.2")
    }

    "have the example heading " in {
      instantiateView()

      insideGovUkWrapper must include("PARAGRAPH 1")
      verify(paragraphBody, atLeastOnce()).apply("some message")
      verify(mockMessages).apply("liability.expectToExceedThresholdDate.example.1")
    }

    "have the example heading message " in {
      instantiateView()

      insideGovUkWrapper must include("PARAGRAPH 1")
      verify(paragraphBody, atLeastOnce()).apply("some message")
      verify(mockMessages).apply("liability.expectToExceedThresholdDate.example.2")
    }

    "have the date input" in {
      instantiateView()

      insideGovUkWrapper must include("GOV UK DATE INPUT")
      verify(govukDateInput).apply(
        DateViewModel(
          field = form("expect-to-exceed-threshold-weight-date"),
          legend = Legend(content = Text("some message"), classes = "govuk-fieldset__legend govuk-fieldset__legend govuk-fieldset__legend--m", isPageHeading = false),
          errors = form.errors
        )(mockMessages)
          .withHint(Hint(content = Text("some message")))
      )

      verify(mockMessages).apply("liability.expectToExceedThresholdDate.question")
      verify(mockMessages).apply("liability.expectToExceedThresholdDate.hint")
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
  def insideGovUkWrapper                       = contentCaptor.getValue.toString

}
