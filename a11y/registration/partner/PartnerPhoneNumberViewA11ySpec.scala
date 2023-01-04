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

package registration.partner

import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.POST
import support.BaseViewSpec
import forms.contact.PhoneNumber
import views.html.partner.partner_phone_number_page

class PartnerPhoneNumberViewA11ySpec extends BaseViewSpec {

  private val page = inject[partner_phone_number_page]
  private val form = PhoneNumber.form()

  private def render(form: Form[PhoneNumber] = form, sectionHeading: Boolean): String =
    page(
      form,
      Call(POST,"/update"),
      "contactName",
      sectionHeading)(journeyRequest, messages).toString()

  "PartnerPhoneNumberViewA11ySpec" should {

    "pass accessibility checks without error" when {
      "is nominated partner" in {
        render(sectionHeading = true) must passAccessibilityChecks
      }

      "not a nominated partner" in {
        render(sectionHeading = false) must passAccessibilityChecks
      }

      "when there is an error" in {
        render(form.withError("error", "message"), true)
      }
    }
  }
}
