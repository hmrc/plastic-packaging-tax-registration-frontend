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

package registration.contact

import play.api.mvc.Call
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.email_address_passcode_confirmation_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ContactDetailsEmailAddressPasscodeConfirmationViewA11ySpec extends BaseViewSpec {

  private val page = inject[email_address_passcode_confirmation_page]

  private val updateCall = Call("GET", "/update")

  private def createView(): String = page(updateCall, Some(messages("primaryContactDetails.sectionHeader"))).toString()

  "Email Address Passcode Confirmation A11y View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}
