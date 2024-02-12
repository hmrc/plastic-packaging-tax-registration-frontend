/*
 * Copyright 2024 HM Revenue & Customs
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

package models.addresslookup

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AddressLookupConfirmationSpec extends AnyWordSpec with Matchers {

  "AddressLookupConfirmation" should {

    def addressLookupConfirmation(lines: List[String]) =
      AddressLookupConfirmation(
        auditRef = "ref",
        id = Some("id"),
        address = AddressLookupAddress(lines = lines, postcode = Some("postcode"), country = None)
      )

    "extract address lines" when {

      "four address lines are returned" in {
        val confirmation = addressLookupConfirmation(List("line1", "line2", "line3", "line4"))
        confirmation.extractAddressLines() mustBe (("line1", Some("line2"), Some("line3"), "line4"))
      }

      "three address lines are returned" in {
        val confirmation = addressLookupConfirmation(List("line1", "line2", "line3"))
        confirmation.extractAddressLines() mustBe (("line1", Some("line2"), None, "line3"))
      }

      "two address lines are returned" in {
        val confirmation = addressLookupConfirmation(List("line1", "line2"))
        confirmation.extractAddressLines() mustBe (("line1", None, None, "line2"))
      }
    }
  }

}
