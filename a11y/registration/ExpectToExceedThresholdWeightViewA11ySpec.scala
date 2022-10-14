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

package registration

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.{ExpectToExceedThresholdWeight, ExpectToExceedThresholdWeightAnswer}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.expect_to_exceed_threshold_weight_page
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers

class ExpectToExceedThresholdWeightViewA11ySpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with Injecting
    with AccessibilityMatchers {

  val request: Request[AnyContent]         = FakeRequest().withCSRFToken
  protected lazy val realMessagesApi: MessagesApi = inject[MessagesApi]

  implicit def messages: Messages = realMessagesApi.preferred(request)
  private val formProvider: ExpectToExceedThresholdWeight = inject[ExpectToExceedThresholdWeight]
  private val page = inject[expect_to_exceed_threshold_weight_page]

  "ExpectToExceedThresholdWeightViewA11ySpec" should {

    def render(form: Form[ExpectToExceedThresholdWeightAnswer]): String =
      page(form)(request, messages).toString()

    "pass accessibility checks without error" in {
      render(formProvider()) must passAccessibilityChecks
    }

    "pass accessibility checks with error" when {
      "no answer "in {
        render(formProvider().bind(Map("answer" -> ""))) must passAccessibilityChecks
      }

      "when date is missing" in {
        render(formProvider().bind(Map("answer" -> "true"))) must passAccessibilityChecks
      }

      "when part of date is missing" in {
        render(formProvider().bind(Map(
          "answer" -> "true",
          "expect-to-exceed-threshold-weight-date.month" -> "12")
        )) must passAccessibilityChecks
      }
    }
  }
}