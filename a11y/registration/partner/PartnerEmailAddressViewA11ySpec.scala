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

package registration.partner


import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.POST
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_email_address_page

class PartnerEmailAddressViewA11ySpec extends BaseViewSpec {

  private val page = inject[partner_email_address_page]
  private val emailAddressForm = EmailAddress.form()

  "PartnerEmailAddressViewA11ySpec" should {

    def render(form: Form[EmailAddress] = emailAddressForm, isNominatedPartner: Boolean): String =
      page(
        form,
        Call(POST, "update"),
        "contactName",
        isNominatedPartner)(journeyRequest, messages).toString()

    "pass accessibility checks without error" when {
      "is nominated partner" in {
        render(isNominatedPartner = true) must passAccessibilityChecks
      }

      "not a nominated partner" in {
        render(isNominatedPartner = false) must passAccessibilityChecks
      }

      "when there is an error" in {
        render(emailAddressForm.withError("error", "message"), true)
      }
    }
  }

}
