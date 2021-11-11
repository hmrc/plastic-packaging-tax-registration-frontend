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

import play.api.libs.json._
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.AddressDetails

case class IncorporationAddressDetails(
  address_line_1: Option[String] = None,
  address_line_2: Option[String] = None,
  locality: Option[String] = None,
  care_of: Option[String] = None,
  po_box: Option[String] = None,
  postal_code: Option[String] = None,
  premises: Option[String] = None,
  country: Option[String] = None
) {

  def toHtml =
    Seq(this.care_of.getOrElse("").trim,
        this.po_box.getOrElse("").trim,
        this.address_line_1.getOrElse("").trim,
        this.address_line_2.getOrElse("").trim,
        this.premises.getOrElse("").trim,
        this.locality.getOrElse("").trim,
        this.postal_code.getOrElse("").trim,
        this.country.getOrElse("").trim
    )
      .filter(_.nonEmpty).mkString("<br>")

  def toPptAddress = {

    val premises = if (this.premises.getOrElse("").nonEmpty) this.premises else None

    premises match {
      case Some(value) =>
        Address(addressLine1 = value.trim,
                addressLine2 = this.address_line_1.getOrElse("").trim,
                addressLine3 = Some(this.address_line_2.getOrElse("").trim),
                townOrCity = this.locality.getOrElse("").trim,
                postCode = this.postal_code.getOrElse("").trim
        )
      case None =>
        Address(addressLine1 = this.address_line_1.getOrElse("").trim,
                addressLine2 = this.address_line_2.getOrElse("").trim,
                townOrCity = this.locality.getOrElse("").trim,
                postCode = this.postal_code.getOrElse("").trim
        )
    }
  }

  def toGroupAddressDetails: AddressDetails = {
    val premises: Option[String] = if (this.premises.getOrElse("").nonEmpty) this.premises else None
    premises match {
      case Some(value) =>
        AddressDetails(addressLine1 = value.trim,
                       addressLine2 = this.address_line_1.getOrElse("").trim,
                       addressLine3 = Some(this.address_line_2.getOrElse("").trim),
                       addressLine4 = Some(this.locality.getOrElse("").trim),
                       postalCode = Some(this.postal_code.getOrElse("").trim),
                       countryCode = this.country.getOrElse("GB")
        )
      case None =>
        AddressDetails(addressLine1 = this.address_line_1.getOrElse("").trim,
                       addressLine2 = this.address_line_2.getOrElse("").trim,
                       addressLine3 = Some(this.locality.getOrElse("").trim),
                       postalCode = Some(this.postal_code.getOrElse("").trim),
                       countryCode = this.country.getOrElse("GB")
        )
    }
  }

}

object IncorporationAddressDetails {

  implicit val format: Format[IncorporationAddressDetails] =
    Json.format[IncorporationAddressDetails]

}
