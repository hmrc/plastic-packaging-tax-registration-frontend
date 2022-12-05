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

import play.api.mvc.Call
import play.api.test.Helpers.GET
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.task_list_group

class TaskListGroupViewA11ySpec extends BaseViewSpec {

  private val page = inject[task_list_group]
  private def render( hasOldLiabilityQuestions: Boolean) =
    page(aRegistration(), Call(GET,"/foo"), hasOldLiabilityQuestions)(journeyRequest, messages).toString()


  "TaskListGroupView" should {
    "pass accessibility check" in {
      render(true) must passAccessibilityChecks
    }

    "pass accessibility check with old liability questions" in {
      render(false) must passAccessibilityChecks
    }
  }
}
