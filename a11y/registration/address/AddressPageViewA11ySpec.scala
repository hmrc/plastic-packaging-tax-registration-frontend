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

package registration.address

import play.api.mvc.Call
import play.api.test.Helpers.GET
import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.address.address_page

class AddressPageViewA11ySpec extends BaseViewSpec {

  private val page = inject[address_page]

  "AddressPageViewA11ySpec" should {

    val input = Map(
      "addressLine1" -> "Address Line 1",
      "addressLine2" -> "Address Line 2",
      "addressLine3" -> "Address Line 3",
      "townOrCity"   -> "Town or City",
      "postCode"     -> "LS4 1RH",
      "countryCode"  -> "GB"
    )

    def render: String =
      page(
        Address.form().bind(input),
        Map("countryCode" -> "any"),
        Call(GET, "/foo"),
        "heading",
        Some("entityName")
      )(request, messages).toString()

    "pass accessibility checks without error" in {
      render must passAccessibilityChecks
    }
  }
}
