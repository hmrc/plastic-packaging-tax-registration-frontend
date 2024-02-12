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
import org.mockito.Mockito.{times, verify, when}
import org.mockito.MockitoSugar.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
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
import views.html.liability.expect_to_exceed_threshold_weight_page
import views.html.main_template
import views.viewmodels.Title
import views.viewmodels.govuk.radios._

class ExpectToExceedThresholdWeightViewSpec extends PlaySpec with BeforeAndAfterEach {

  val request = FakeRequest()
  val mockMessages = mock[Messages]

  val form: Form[Boolean] = Form[Boolean]("value" -> ignored[Boolean](true))
  val sectionHeader = mock[sectionHeader]
  val pageHeading = mock[pageHeading]
  val govUkLayout = mock[main_template]
  val contentCaptor = ArgumentCaptor.forClass(classOf[Html])
  val saveButtons = mock[saveButtons]
  val errorSummary = mock[errorSummary]
  val govukRadios = mock[GovukRadios]
  val paragraphBody = mock[paragraphBody]
  val inset = mock[inset]
  val bulletList = mock[bulletList]
  val link = mock[link]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMessages, sectionHeader, pageHeading, govUkLayout, saveButtons, errorSummary, govukRadios, paragraphBody, inset, bulletList, link)

    when(mockMessages.apply(anyString(), any())).thenReturn("some message")
    when(sectionHeader.apply(any())).thenReturn(HtmlFormat.raw("SECTION HEADER"))
    when(pageHeading.apply(any(), any(), any())).thenReturn(HtmlFormat.raw("PAGE HEADING"))
    when(govUkLayout.apply(any(), any(), any())(contentCaptor.capture())(any(), any())).thenReturn(HtmlFormat.raw("GOVUK"))
    when(saveButtons.apply(any())(any())).thenReturn(HtmlFormat.raw("SAVE BUTTONS"))
    when(errorSummary.apply(any(), any())(any())).thenReturn(HtmlFormat.raw("ERROR SUMMARY"))
    when(govukRadios.apply(any())).thenReturn(HtmlFormat.raw("GOV UK RADIOS"))
    when(paragraphBody.apply(any(), any(), any())).thenReturn(HtmlFormat.raw("PARAGRAPH 0"), Seq(1, 2, 3).map(i => HtmlFormat.raw(s"PARAGRAPH $i")):_*)
    when(inset.apply(any())).thenReturn(HtmlFormat.raw("GOV UK INSET"))
    when(bulletList.apply(any())).thenReturn(HtmlFormat.raw("BULLET LIST"))
    when(link.apply(any(), any(), any(), any(), any(), any())).thenReturn(HtmlFormat.raw("LINK"))
  }

  private val page = new expect_to_exceed_threshold_weight_page(
    formHelper = new FormWithCSRF,
    sectionHeader = sectionHeader,
    pageHeading = pageHeading,
    govukLayout = govUkLayout,
    saveButtons = saveButtons,
    errorSummary = errorSummary,
    govukRadios = govukRadios,
    paragraphBody = paragraphBody,
    inset = inset,
    bulletList = bulletList,
    link = link
  )

  "view" must {
    "use govUk layout" in {
      instantiateView()

      verify(govUkLayout).apply(
        refEq(Title("liability.expectToExceedThresholdWeight.title")),
        any(),
        any())(any())(meq(request), meq(mockMessages))
    }

    "have the form" in {
      instantiateView()

      val form = Jsoup.parse(insideGovUkWrapper).getElementsByTag("form").first()
      form.attr("method") mustBe controllers.liability.routes.ExpectToExceedThresholdWeightController.submit().method
      form.attr("action") mustBe controllers.liability.routes.ExpectToExceedThresholdWeightController.submit().url
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
      verify(mockMessages).apply("liability.expectToExceedThresholdWeight.title")
    }

    "have the first paragraph" in {
      instantiateView()

      insideGovUkWrapper must include("PARAGRAPH 0")
      verify(mockMessages).apply("liability.expectToExceedThresholdWeight.p1")
    }

    "have the inset" in {
      instantiateView()

      insideGovUkWrapper must include("GOV UK INSET")
      verify(inset, times(1)).apply(HtmlFormat.raw("PARAGRAPH 1"), HtmlFormat.raw("BULLET LIST"))
      verify(mockMessages).apply("liability.expectToExceedThresholdWeight.inset.p1")
    }

    "have the bullet list" in {
      instantiateView()

      verify(bulletList).apply(Seq(HtmlFormat.raw("PARAGRAPH 2"), HtmlFormat.raw("PARAGRAPH 3")))
      verify(mockMessages).apply("liability.expectToExceedThresholdWeight.inset.bullet.1")
      verify(mockMessages).apply("liability.expectToExceedThresholdWeight.inset.bullet.2")
    }

    "have the link" in {
      instantiateView()

      verify(link).apply(meq("some message"), meq(Call("GET", "https://www.gov.uk/guidance/when-you-must-register-for-plastic-packaging-tax#when-to-register")), meq(false), any(), any(), any())
      verify(mockMessages).apply("liability.expectToExceedThresholdWeight.p3", HtmlFormat.raw("LINK"))
      verify(mockMessages).apply("liability.expectToExceedThresholdWeight.p3.link")
    }

    "have the radio buttons" in {
      instantiateView()

      insideGovUkWrapper must include("GOV UK RADIOS")

      verify(govukRadios).apply(
        RadiosViewModel.yesNo(
          field = form("value"),
          legend = Legend(
            content = Text("some message"),
            classes = "govuk-fieldset__legend govuk-fieldset__legend govuk-fieldset__legend--m",
            isPageHeading = false
          )
        )(mockMessages).inline().withHint(Hint(content = Text("some message")))
      )

      verify(mockMessages).apply("liability.expectToExceedThresholdWeight.question")
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
  def insideGovUkWrapper = contentCaptor.getValue.toString

}
