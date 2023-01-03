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

package registration.contact

import support.BaseViewSpec
import views.html.contact.too_many_attempts_passcode_page

class TooManyAttemptsPasscodeViewA11ySpec extends BaseViewSpec {

  private val page = inject[too_many_attempts_passcode_page]

  private def createView(): String = page()(request = journeyRequest, messages = messages).toString()

  "Too Many Attempts Passcode A11y View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}
