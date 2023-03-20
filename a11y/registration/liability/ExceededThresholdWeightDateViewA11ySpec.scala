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

import forms.liability.ExceededThresholdWeightDate
import play.api.data.Form
import support.BaseViewSpec
import views.html.liability.exceeded_threshold_weight_date_page

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

class ExceededThresholdWeightDateViewA11ySpec extends BaseViewSpec {

  private val fakeClock = {
    Clock.fixed(Instant.parse("2022-06-01T12:00:00Z"), TimeZone.getDefault.toZoneId)
  }

  private val page = inject[exceeded_threshold_weight_date_page]
  private val formProvider = new ExceededThresholdWeightDate(appConfig, fakeClock)
  private val form = formProvider()(messages)

  private def render(form: Form[LocalDate] = form) : String =
    page(form)(journeyRequest, messages).toString()

  "view" should {
    "pass accessibility checks without error" in {
      render() must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val bindForm = form.bind(Map("expect-to-exceed-threshold-weight-date"-> "") )

      render(bindForm) must passAccessibilityChecks
    }
  }

}
