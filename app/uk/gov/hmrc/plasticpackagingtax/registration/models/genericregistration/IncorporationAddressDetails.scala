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
  address_line_1: Option[String] = None,
  address_line_2: Option[String] = None,
  locality: Option[String] = None,
  care_of: Option[String] = None,
  po_box: Option[String] = None,
  postal_code: Option[String] = None,
  premises: Option[String] = None,
  region: Option[String] = None,
  country: Option[String] = None
) {

  def toHtml =
    Seq(this.care_of.getOrElse("").trim,
        this.po_box.getOrElse("").trim,
        this.address_line_1.getOrElse("").trim,
        this.address_line_2.getOrElse("").trim,
        this.premises.getOrElse("").trim,
        this.locality.getOrElse("").trim,
        this.region.getOrElse("").trim,
        this.postal_code.getOrElse("").trim,
        this.country.getOrElse("").trim
    )
      .filter(_.nonEmpty).mkString("<br>")

  def toPptAddress = {
    val addressLineOne = {
      if (this.address_line_1.getOrElse("").nonEmpty) this.address_line_1
      else if (this.po_box.getOrElse("").nonEmpty) this.po_box
      else this.premises
    }

    Address(addressLine1 = addressLineOne.getOrElse("").trim,
            addressLine2 = Some(this.address_line_2.getOrElse("").trim),
            townOrCity = this.locality.getOrElse("").trim,
            postCode = this.postal_code.getOrElse("").trim,
            county = Some(region.getOrElse("").trim)
    )
  }

}

object IncorporationAddressDetails {

  val apiReads: Reads[IncorporationAddressDetails] = (
    (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_1").readNullable[
      String
    ].orElse(Reads.pure(None)) and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_2").readNullable[
        String
      ].orElse(Reads.pure(None)) and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "locality").readNullable[String].orElse(
        Reads.pure(None)
      ) and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "care_of").readNullable[String].orElse(
        Reads.pure(None)
      ) and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "po_box").readNullable[String].orElse(
        Reads.pure(None)
      ) and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "postal_code").readNullable[
        String
      ].orElse(Reads.pure(None)) and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "premises").readNullable[String].orElse(
        Reads.pure(None)
      ) and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "region").readNullable[String].orElse(
        Reads.pure(None)
      ) and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "country").readNullable[String].orElse(
        Reads.pure(None)
      )
  )(IncorporationAddressDetails.apply _)

  val apiWrites: Writes[IncorporationAddressDetails] = (
    (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_1").writeNullable[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "address_line_2").writeNullable[
        String
      ] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "locality").writeNullable[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "care_of").writeNullable[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "po_box").writeNullable[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "postal_code").writeNullable[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "premises").writeNullable[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "region").writeNullable[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress" \ "country").writeNullable[String]
  )(unlift(IncorporationAddressDetails.unapply))

  val apiFormat: Format[IncorporationAddressDetails] =
    Format[IncorporationAddressDetails](apiReads, apiWrites)

  implicit val format: Format[IncorporationAddressDetails] =
    Json.format[IncorporationAddressDetails]

}
