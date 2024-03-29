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

package registration.liability

import forms.liability.ExceededThresholdWeight
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers
import views.html.liability.exceeded_threshold_weight_page

class ExceededThresholdWeightViewA11ySpec
  extends PlaySpec
    with GuiceOneAppPerSuite
    with Injecting
    with AccessibilityMatchers {

  val request: Request[AnyContent]         = FakeRequest().withCSRFToken
  protected lazy val realMessagesApi: MessagesApi = inject[MessagesApi]

  implicit def messages: Messages = realMessagesApi.preferred(request)
  private val formProvider: ExceededThresholdWeight = inject[ExceededThresholdWeight]
  private val page = inject[exceeded_threshold_weight_page]

  "ExpectToExceedThresholdWeightViewA11ySpec" should {

    def render(form: Form[Boolean]): String =
      page(form)(request, messages).toString()

    "pass accessibility checks without error" in {
      render(formProvider()) must passAccessibilityChecks
    }

    "pass accessibility checks with error" when {
      "no answer "in {
        render(formProvider().bind(Map("value" -> ""))) must passAccessibilityChecks
      }

    }
  }
}