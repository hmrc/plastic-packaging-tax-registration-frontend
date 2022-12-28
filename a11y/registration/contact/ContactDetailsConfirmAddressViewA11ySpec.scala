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
import support.BaseViewSpec
import forms.contact.ConfirmAddress
import models.genericregistration.IncorporationAddressDetails
import views.html.contact.confirm_address

class ContactDetailsConfirmAddressViewA11ySpec extends BaseViewSpec {

  private val page = inject[confirm_address]

  private val incorporationAddressDetails = IncorporationAddressDetails(
    address_line_1 = Some("testLine1"),
    address_line_2 = Some("testLine2"),
    locality = Some("test town"),
    care_of = Some("test name"),
    po_box = Some("123"),
    postal_code = Some("AA11AA"),
    premises = Some("1"),
    country = Some("United Kingdom")
  )

  private def createView(form: Form[ConfirmAddress] = ConfirmAddress.form(), isGroup: Boolean = false): String = {
    page(form, addressConversionUtils.toPptAddress(incorporationAddressDetails), isGroup)(journeyRequest, messages).toString()
  }

  "Confirm Address View" should {

    val view = createView()

    "pass accessibility checks without error" when {
      "Single organisation" in {
        view must passAccessibilityChecks
      }

      "Group organisation" in {
        val view = createView(isGroup = true)
        view must passAccessibilityChecks
      }
    }
  }
}
