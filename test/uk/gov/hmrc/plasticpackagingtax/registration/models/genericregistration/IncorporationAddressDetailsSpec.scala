/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsObject

class IncorporationAddressDetailsSpec extends AnyWordSpec with Matchers {
  "IncorporationAddressDetails" should {
    "convert to Address" when {
      "premises is provided" in {
        val incorporationAddressDetails = IncorporationAddressDetails(address_line_1 =
                                                                        Some("test line 1"),
                                                                      address_line_2 =
                                                                        Some("test line 2   "),
                                                                      locality =
                                                                        Some("test town  "),
                                                                      care_of = Some("  test user"),
                                                                      postal_code = Some("AC1 23C"),
                                                                      premises =
                                                                        Some("Warehouse 1"),
                                                                      country = Some("GB")
        )
        incorporationAddressDetails.toPptAddress.addressLine1 mustBe incorporationAddressDetails.premises.get
        incorporationAddressDetails.toPptAddress.addressLine2 mustBe incorporationAddressDetails.address_line_1.map(
          _.trim
        )
        incorporationAddressDetails.toPptAddress.addressLine3.get mustBe incorporationAddressDetails.address_line_2.get.trim
        incorporationAddressDetails.toPptAddress.postCode mustBe incorporationAddressDetails.postal_code
        incorporationAddressDetails.toPptAddress.townOrCity mustBe incorporationAddressDetails.locality.get.trim
      }

      "premises not provided" in {
        val incorporationAddressDetails = IncorporationAddressDetails(address_line_1 =
                                                                        Some("test line 1"),
                                                                      address_line_2 =
                                                                        Some("test line 2   "),
                                                                      locality =
                                                                        Some("test town  "),
                                                                      care_of = Some("  test user"),
                                                                      postal_code = Some("AC1 23C"),
                                                                      country = Some("GB")
        )
        incorporationAddressDetails.toPptAddress.addressLine1 mustBe incorporationAddressDetails.address_line_1.get
        incorporationAddressDetails.toPptAddress.addressLine2 mustBe incorporationAddressDetails.address_line_2.map(
          _.trim
        )
        incorporationAddressDetails.toPptAddress.addressLine3 mustBe None
        incorporationAddressDetails.toPptAddress.postCode mustBe incorporationAddressDetails.postal_code
        incorporationAddressDetails.toPptAddress.townOrCity mustBe incorporationAddressDetails.locality.get.trim
      }

      "apply json payload" when {
        "payload is not available" in {
          val incorporationAddressDetails =
            IncorporationAddressDetails.format.reads(JsObject.empty).get
          incorporationAddressDetails.address_line_1 mustBe None
          incorporationAddressDetails.address_line_2 mustBe None
          incorporationAddressDetails.locality mustBe None
          incorporationAddressDetails.care_of mustBe None
          incorporationAddressDetails.po_box mustBe None
          incorporationAddressDetails.premises mustBe None
          incorporationAddressDetails.postal_code mustBe None
          incorporationAddressDetails.country mustBe None
        }
      }
    }

  }
}
