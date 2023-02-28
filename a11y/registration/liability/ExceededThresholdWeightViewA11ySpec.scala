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
import play.api.data.Form
import support.BaseViewSpec
import views.html.liability.exceeded_threshold_weight_page

import java.time.{Clock, Instant}
import java.util.TimeZone

class ExceededThresholdWeightViewA11ySpec extends BaseViewSpec {

  private val page = inject[exceeded_threshold_weight_page]

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-06-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

//  private val form = new ExceededThresholdWeight(appConfig, fakeClock).form()

//  private def render(form: Form[ExceededThresholdWeightAnswer] = form): String =
//    page(form, true)(journeyRequest, messages).toString()

  "ExceededThresholdWeight View" should {

//    "pass accessibility checks without error" in {
//      render() must passAccessibilityChecks
//    }
//
//    "pass accessibility checks with error" in {
//      render(form.bind(Map("answer" -> ""))) must passAccessibilityChecks
//    }
  }
}
