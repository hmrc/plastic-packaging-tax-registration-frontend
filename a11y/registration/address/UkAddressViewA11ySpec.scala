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

package registration.address

import play.api.data.Form
import support.BaseViewSpec
import forms.address.UkAddressForm
import views.html.address.uk_address_page

class UkAddressViewA11ySpec extends BaseViewSpec {

  private val page = inject[uk_address_page]

  "UkAddressViewA11ySpec" should {

    def createView(form: Form[Boolean] ): String = {
      page(form, Some("entity"), "addressCapture.contact.heading.isUK")
    }.toString()

    "pass accessibility checks without error" in {
      createView(UkAddressForm.form()) must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = UkAddressForm
        .form()
        .bind(Map("ukAddress" -> ""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }
}
