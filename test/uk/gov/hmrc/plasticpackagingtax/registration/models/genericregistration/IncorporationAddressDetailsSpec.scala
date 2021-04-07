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

class IncorporationAddressDetailsSpec extends AnyWordSpec with Matchers {
  "IncorporationAddressDetails" should {
    "override address line 1 with address_line_1 if provided" in {
      val incorporationAddressDetails = IncorporationAddressDetails(address_line_1 = "test line1",
                                                                    address_line_2 = "test line 2",
                                                                    locality = "test town",
                                                                    care_of = "test user",
                                                                    po_box = "PO 123",
                                                                    postal_code = "AC1 23C",
                                                                    premises = "Warehouse 1",
                                                                    region = "test county",
                                                                    country = "GB"
      )
      incorporationAddressDetails.toPptAddress.addressLine1 mustBe incorporationAddressDetails.address_line_1
    }

    "override address line 1 with PO Box if provided" in {
      val incorporationAddressDetails = IncorporationAddressDetails(address_line_1 = "",
                                                                    address_line_2 = "test line 2",
                                                                    locality = "test town",
                                                                    care_of = "test user",
                                                                    po_box = "PO 123",
                                                                    postal_code = "AC1 23C",
                                                                    premises = "Warehouse 1",
                                                                    region = "test county",
                                                                    country = "GB"
      )
      incorporationAddressDetails.toPptAddress.addressLine1 mustBe incorporationAddressDetails.po_box
    }

    "override address line 1 with premises if provided" in {
      val incorporationAddressDetails = IncorporationAddressDetails(address_line_1 = "",
                                                                    address_line_2 =
                                                                      "test line 2   ",
                                                                    locality = "test town  ",
                                                                    care_of = "  test user",
                                                                    po_box = "",
                                                                    postal_code = "AC1 23C",
                                                                    premises = "Warehouse 1",
                                                                    region = "test county",
                                                                    country = "GB"
      )
      incorporationAddressDetails.toPptAddress.addressLine1 mustBe incorporationAddressDetails.premises
    }
  }
}
