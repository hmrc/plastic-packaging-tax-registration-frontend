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

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json._
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Address

case class IncorporationAddressDetails(
  address_line_1: String,
  address_line_2: String,
  locality: String,
  care_of: String,
  po_box: String,
  postal_code: String,
  premises: String,
  region: String,
  country: String
) {

  def toHtml =
    Seq(this.care_of,
        this.po_box,
        this.address_line_1,
        this.address_line_2,
        this.premises,
        this.locality,
        this.region,
        this.postal_code,
        this.country
    )
      .filter(_.nonEmpty).mkString("<br>")

  def toPptAddress = {
    val addressLineOne = {
      if (this.address_line_1.nonEmpty) this.address_line_1
      else if (this.po_box.nonEmpty) this.po_box
      else this.premises
    }

    Address(addressLine1 = addressLineOne.trim,
            addressLine2 = Some(this.address_line_2.trim),
            townOrCity = this.locality.trim,
            postCode = this.postal_code.trim,
            county = Some(region.trim)
    )
  }

}

object IncorporationAddressDetails {

  val apiReads: Reads[IncorporationAddressDetails] = (
    (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_1").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_2").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "locality").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "care_of").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "po_box").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "postal_code").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "premises").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "region").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "country").read[String]
  )(IncorporationAddressDetails.apply _)

  val apiWrites: Writes[IncorporationAddressDetails] = (
    (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_1").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_2").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "locality").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "care_of").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "po_box").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "postal_code").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "premises").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "region").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "country").write[String]
  )(unlift(IncorporationAddressDetails.unapply))

  val apiFormat: Format[IncorporationAddressDetails] =
    Format[IncorporationAddressDetails](apiReads, apiWrites)

  implicit val format: Format[IncorporationAddressDetails] =
    Json.format[IncorporationAddressDetails]

}
