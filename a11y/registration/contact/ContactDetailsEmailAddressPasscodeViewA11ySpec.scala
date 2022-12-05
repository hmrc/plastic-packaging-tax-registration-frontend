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

import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.POST
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddressPasscode
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.email_address_passcode_page

class ContactDetailsEmailAddressPasscodeViewA11ySpec extends BaseViewSpec {

  private val page = inject[email_address_passcode_page]
  private val form = EmailAddressPasscode.form()

  private def render(form: Form[EmailAddressPasscode]) =
    page(
      form,
      Some("emailAddress"),
      Call(POST, "/update"),
      Some("heading")
    ).toString()

  "EmailAddressPasscodeViewA11ySpec" should {
    "pass accessibility test" in {
      render(form) must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      render(form.withError("error", "message")) must passAccessibilityChecks
    }
  }

}
