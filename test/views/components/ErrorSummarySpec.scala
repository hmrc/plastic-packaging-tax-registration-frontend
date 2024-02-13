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

package views.components

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.{ErrorLink, ErrorSummary, Text}
import uk.gov.hmrc.govukfrontend.views.html.components.GovukErrorSummary
import views.html.components.errorSummary

class ErrorSummarySpec extends PlaySpec with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGovukErrorSummary, mockMessages)
  }

  val mockGovukErrorSummary = mock[GovukErrorSummary]
  val mockMessages          = mock[Messages]

  val sut = new errorSummary(mockGovukErrorSummary)

  "apply" must {
    "display nothing" when {
      "there are no errors" in {
        val html         = sut.apply(Seq.empty)(mockMessages)
        val errorSummary = Jsoup.parse(html.toString).body().children()

        assert(errorSummary.isEmpty)
      }
    }

    "display the govUkErrorSummary" when {
      val errors = Seq(FormError("error.key", "error.message.key"))
      "using the form error key" in {
        when(mockGovukErrorSummary.apply(any())).thenReturn(HtmlFormat.raw("<p>TEST ERROR SUMMARY</p>"))
        when(mockMessages.apply(meq("error.message.key"), any())).thenReturn("test-error-message")
        when(mockMessages.apply(meq("site.error.summary.title"), any())).thenReturn("error-title")

        val html         = sut.apply(errors)(mockMessages)
        val errorSummary = Jsoup.parse(html.toString).body().children()

        errorSummary.text() mustBe "TEST ERROR SUMMARY"
        verify(mockGovukErrorSummary).apply(ErrorSummary(errorList = Seq(ErrorLink(href = Some("#error.key"), content = Text("test-error-message"))), title = Text("error-title")))
      }

      "using the override error key" in {
        when(mockGovukErrorSummary.apply(any())).thenReturn(HtmlFormat.raw("<p>TEST ERROR SUMMARY OVERRIDE</p>"))
        when(mockMessages.apply(meq("error.message.key"), any())).thenReturn("test-error-message")
        when(mockMessages.apply(meq("site.error.summary.title"), any())).thenReturn("error-title")

        val html         = sut.apply(errors, Some("override-key"))(mockMessages)
        val errorSummary = Jsoup.parse(html.toString).body().children()

        errorSummary.text() mustBe "TEST ERROR SUMMARY OVERRIDE"
        verify(mockGovukErrorSummary).apply(
          ErrorSummary(errorList = Seq(ErrorLink(href = Some("#override-key"), content = Text("test-error-message"))), title = Text("error-title"))
        )
      }
    }
  }

}
