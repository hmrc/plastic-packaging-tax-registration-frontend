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

package forms.contact

import base.unit.CommonTestUtils
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import play.api.libs.json.Json
import forms.contact.Address.UKAddress
import models.addresslookup.{AddressLookupAddress, AddressLookupConfirmation, AddressLookupCountry}
import utils.AddressConversionUtils

class AddressSpec extends AnyWordSpec with Matchers with CommonTestUtils with GuiceOneAppPerSuite {

  val addressConversionUtils: AddressConversionUtils =
    app.injector.instanceOf[AddressConversionUtils]

  "Address validation rules" should {

    "return success" when {

      "all address fields are valid" in {

        val input = Map(
          "addressLine1" -> "Address Line 1",
          "addressLine2" -> "Address Line 2",
          "addressLine3" -> "Address Line 3",
          "townOrCity"   -> "Town or City",
          "postCode"     -> "LS4 1RH",
          "countryCode"  -> "GB"
        )

        val form = Address.form().bind(input)
        form.errors.size mustBe 0
      }

      "address mandatory fields are valid" in {

        val input = Map(
          "addressLine1" -> "Address Line 1 .'-&",
          "addressLine2" -> "Address Line 2 .'-&",
          "townOrCity"   -> "Town or City .'-&",
          "postCode"     -> "LS4 1RH",
          "countryCode"  -> "GB"
        )

        val form = Address.form().bind(input)
        form.errors.size mustBe 0
      }

      "address mandatory fields with lower case post code are valid " in {

        val input = Map(
          "addressLine1" -> "Address Line 1 .'-&",
          "addressLine2" -> "Address Line 2 .'-&",
          "townOrCity"   -> "Town or City .'-&",
          "postCode"     -> "ls4 1rh",
          "countryCode"  -> "GB"
        )

        val form = Address.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "mandatory fields provided with empty data" in {

        val input =
          Map("addressLine1" -> "", "townOrCity" -> "", "postCode" -> "", "countryCode" -> "GB")
        val expectedErrors =
          Seq(
            FormError("addressLine1", "primaryContactDetails.address.addressLine1.empty.error"),
            FormError("townOrCity", "primaryContactDetails.address.townOrCity.empty.error"),
            FormError("postCode", "primaryContactDetails.address.postCode.empty.error")
          )

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains incorrect data" in {

        val input = Map(
          "addressLine1" -> "Address Line 1888888888888888888888888888888888888",
          "addressLine2" -> "Address Line 2%%$%$%$%",
          "addressLine3" -> "Address Line 3**********",
          "townOrCity"   -> "Town or City££$£$£$+",
          "postCode"     -> "LS4 1RH £$£$£$++---",
          "countryCode"  -> "GB"
        )
        val expectedErrors =
          Seq(
            FormError("addressLine1", "primaryContactDetails.address.addressLine1.format.error"),
            FormError("addressLine2", "primaryContactDetails.address.addressLine2.format.error"),
            FormError("addressLine3", "primaryContactDetails.address.addressLine3.format.error"),
            FormError("townOrCity", "primaryContactDetails.address.townOrCity.format.error"),
            FormError("postCode", "primaryContactDetails.address.postCode.format.error")
          )

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains invalid postcode" in {

        val input = Map(
          "addressLine1" -> "Address Line 1",
          "addressLine2" -> "Address Line 2",
          "addressLine3" -> "Address Line 3",
          "townOrCity"   -> "Town ",
          "postCode"     -> "LSA41RH",
          "countryCode"  -> "GB"
        )
        val expectedErrors =
          Seq(FormError("postCode", "primaryContactDetails.address.postCode.format.error"))

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "be created from AddressLookupConfirmation" when {

      def addressLookupConfirmation(lines: List[String], postcode: Option[String], country: Option[AddressLookupCountry] = None) =
        AddressLookupConfirmation(
          auditRef = "ref",
          id = Some("id"),
          address = AddressLookupAddress(lines = lines, postcode = postcode, country = country)
        )

      "four address lines and a postcode are returned" in {
        val address = addressConversionUtils.toPptAddress(addressLookupConfirmation(List("line1", "line2", "line3", "town"), Some("postCode")))
        address.addressLine1 mustBe "line1"
        address.addressLine2 mustBe Some("line2")
        address.addressLine3 mustBe Some("line3")
        address.townOrCity mustBe "town"
        address.maybePostcode mustBe Some("postCode")
      }

      "three address lines and postcode are returned" in {
        val address = addressConversionUtils.toPptAddress(addressLookupConfirmation(List("line1", "line2", "town"), Some("postCode")))
        address.addressLine1 mustBe "line1"
        address.addressLine2 mustBe Some("line2")
        address.addressLine3 mustBe None
        address.townOrCity mustBe "town"
        address.maybePostcode mustBe Some("postCode")
      }

      "two address lines and a postcode is returned" in {
        val address = addressConversionUtils.toPptAddress(addressLookupConfirmation(List("line1", "town"), Some("postCode")))
        address.addressLine1 mustBe "line1"
        address.addressLine2 mustBe None
        address.addressLine3 mustBe None
        address.townOrCity mustBe "town"
        address.maybePostcode mustBe Some("postCode")
      }

      "address with overseas country is returned" in {
        val address =
          addressConversionUtils.toPptAddress(addressLookupConfirmation(List("line1", "town"), None, Some(AddressLookupCountry("IE", "Ireland"))))
        address.addressLine1 mustBe "line1"
        address.addressLine2 mustBe None
        address.addressLine3 mustBe None
        address.townOrCity mustBe "town"
        address.countryCode mustBe "IE"
      }
    }
  }

  "Address" should {
    //To handle invalid address state saved before live issue fix on 18th May 2022
    "read a UKAddress with an empty string from Json when there is no postcode" in {

      val gbAddressNoPostcode = Json.obj(
        "addressLine1" -> "testLine1",
        "addressLine2" -> "testLine2",
        "addressLine3" -> "testLine3",
        "townOrCity" -> "town",
        "countryCode" -> "GB"
      )

      gbAddressNoPostcode.as[Address] mustBe UKAddress(
        addressLine1 = "testLine1",
        addressLine2 = Some("testLine2"),
        addressLine3 = Some("testLine3"),
        townOrCity = "town",
        postCode = ""
      )
    }

  }

  def testFailedValidationErrors(input: Map[String, String], expectedErrors: Seq[FormError]): Unit = {
    val form = Address.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
  }

}
