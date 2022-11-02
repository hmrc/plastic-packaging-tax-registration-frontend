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

package registration.deregistration

import play.api.data.Form
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.deregistration.DeregisterReasonForm
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.deregistration.deregister_reason_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class DeregisterReasonViewA11ySpec extends BaseViewSpec {

  private val page = inject[deregister_reason_page]

  private def createView(form: Form[DeregisterReasonForm] = DeregisterReasonForm.form()): String =
    page(form)(authenticatedRequest, messages).toString()

  "Confirm Deregistration Reason View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = DeregisterReasonForm.form().bind(Map("value" -> ""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }
}
