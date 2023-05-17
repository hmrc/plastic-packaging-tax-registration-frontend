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

package registration.deregistration

import support.BaseViewSpec
import models.deregistration.DeregistrationDetails
import views.html.deregistration.deregister_check_your_answers_page

class DeregisterCheckYourAnswersViewA11ySpec extends BaseViewSpec {

  private val page = inject[deregister_check_your_answers_page]

  private def createView(deregistrationDetails: DeregistrationDetails): String =
    page(deregistrationDetails)(registrationRequest, messages.messages).toString()

  "Deregister Check Answers View with no answers" should {

    val view = createView(DeregistrationDetails(None, None))

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}
