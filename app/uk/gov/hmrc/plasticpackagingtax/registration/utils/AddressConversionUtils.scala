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

package uk.gov.hmrc.plasticpackagingtax.registration.utils

import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.AddressLookupConfirmation
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationAddressDetails
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService

import javax.inject.Inject

class AddressConversionUtils @Inject() (countryService: CountryService) {

  def toPptAddress(incAddress: IncorporationAddressDetails): Option[Address] = {

    def getCountryCode: Option[String] =
      incAddress.country.flatMap {
        countryService.getKeyForName
      }

    val linesOneToThree =
      Seq(incAddress.premises, incAddress.address_line_1, incAddress.address_line_2)
        .flatten.map(_.trim).filter(_.nonEmpty)

    getCountryCode map { country =>
      Address(
        addressLine1 = linesOneToThree.headOption.getOrElse(""),
        addressLine2 = linesOneToThree.lift(1),
        addressLine3 = linesOneToThree.lift(2),
        townOrCity = incAddress.locality.getOrElse("").trim,
        maybePostcode = incAddress.postal_code.map(_.trim),
        countryCode = country
      )
    }
  }

  def toPptAddress(addressLookupConfirmation: AddressLookupConfirmation): Address = {
    val lines                    = addressLookupConfirmation.extractAddressLines()
    val addressLookupCountryCode = addressLookupConfirmation.address.country.map(_.code).getOrElse("GB")

    Address(lines._1, lines._2, lines._3, lines._4, addressLookupConfirmation.address.postcode, addressLookupCountryCode)

  }

}
