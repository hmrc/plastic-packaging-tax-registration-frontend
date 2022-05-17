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

package utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address.UKAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationAddressDetails
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.utils.AddressConversionUtils

class AddressConversionUtilsSpec extends AnyWordSpec with Matchers {

  val acu = new AddressConversionUtils(new CountryService)

  "toPptAddress" when {
    "provided with an incorporation address with no postcode and country of United Kingdom" should {

      "generate a UKAddress with an empty string for postcode" in {

        val stubRegisteredOfficeAddressNoPostcode = Json.obj(
          "address_line_1" -> "testLine1",
          "address_line_2" -> "test town",
          "care_of" -> "test name",
          "country" -> "United Kingdom",
          "locality" -> "test city",
          "po_box" -> "123",
          "premises" -> "1",
          "region" -> "test region"
        )

        val incAddress = stubRegisteredOfficeAddressNoPostcode.as[IncorporationAddressDetails]

        acu.toPptAddress(incAddress) mustBe Some(UKAddress(
          "1",
          Some("testLine1"),
          Some("test town"),
          "test city",
          ""
        ))

      }
    }

    "provided with an incorporation address with a postcode and country of United Kingdom" should {
      "generate a UKAddress" in {

        val stubRegisteredOfficeAddressWithPostcode = Json.obj(
          "address_line_1" -> "testLine1",
          "address_line_2" -> "test town",
          "care_of" -> "test name",
          "country" -> "United Kingdom",
          "locality" -> "test city",
          "postal_code" -> "AA11AA",
          "po_box" -> "123",
          "premises" -> "1",
          "region" -> "test region"
        )

        val incAddress = stubRegisteredOfficeAddressWithPostcode.as[IncorporationAddressDetails]

        acu.toPptAddress(incAddress) mustBe Some(UKAddress(
          "1",
          Some("testLine1"),
          Some("test town"),
          "test city",
          "AA11AA"
        ))

      }
    }
  }
}
