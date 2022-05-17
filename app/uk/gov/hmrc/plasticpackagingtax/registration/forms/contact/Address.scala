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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.contact

import play.api.data.Forms.{optional, text}
import play.api.data.{Form, Forms, Mapping}
import play.api.libs.json._
import uk.gov.hmrc.plasticpackagingtax.registration.forms.CommonFormValidators
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

sealed trait Address {
  val addressLine1: String
  val addressLine2: Option[String]
  val addressLine3: Option[String]
  val townOrCity: String
  val maybePostcode: Option[String]
  val countryCode: String

  def isValid: Boolean = countryCode.trim.nonEmpty

  def isUkAndEmptyPostcode: Boolean = countryCode == "GB" && maybePostcode.fold(true)(_.isEmpty)

}

object Address extends CommonFormValidators {

  case class UKAddress(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    townOrCity: String,
    postCode: String,
    countryCode: String = "GB"
  ) extends Address {
    val maybePostcode: Option[String] = Some(postCode)
  }

  case class NonUKAddress(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    townOrCity: String,
    postCode: Option[String],
    countryCode: String
  ) extends Address {
    val maybePostcode: Option[String] = postCode
  }

  val nonUKFormat: OFormat[NonUKAddress] = Json.format[NonUKAddress]
  val ukFormat: OFormat[UKAddress]       = Json.format[UKAddress]

  implicit val reads: Reads[Address] = (__ \ "countryCode").read[String] flatMap {
    case "GB" => ukFormat.widen[Address]
    case _    => nonUKFormat.widen[Address]
  }

  implicit val writes: Writes[Address] = Writes[Address] {
    case ukAddress: UKAddress       => ukFormat.writes(ukAddress)
    case nonUKAddress: NonUKAddress => nonUKFormat.writes(nonUKAddress)
  }

  def apply(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    townOrCity: String,
    maybePostcode: Option[String],
    countryCode: String
  ): Address =
    if (countryCode == "GB")
      UKAddress(
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        addressLine3 = addressLine3,
        townOrCity = townOrCity,
        postCode = maybePostcode.getOrElse("")
      )
    else
      NonUKAddress(
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        addressLine3 = addressLine3,
        townOrCity = townOrCity,
        postCode = maybePostcode,
        countryCode = countryCode
      )

  def unapply(address: Address): Option[(String, Option[String], Option[String], String, Option[String], String)] =
    address match {
      case UKAddress(addressLine1, addressLine2, addressLine3, townOrCity, postCode, countryCode) =>
        Some(Tuple6(addressLine1, addressLine2, addressLine3, townOrCity, Some(postCode), countryCode))
      case NonUKAddress(addressLine1, addressLine2, addressLine3, townOrCity, postCode, countryCode) =>
        Some(Tuple6(addressLine1, addressLine2, addressLine3, townOrCity, postCode, countryCode))
    }

  private val validateAddressField: Int => String => Boolean =
    (length: Int) => (input: String) => isNotExceedingMaxLength(input, length) && isValidAddressInput(input)

  private val addressFieldMaxSize = 35

  private val addressLine1FieldName = "addressLine1"
  private val addressLine2FieldName = "addressLine2"
  private val addressLine3FieldName = "addressLine3"
  private val townOrCityFieldName   = "townOrCity"
  private val postCodeFieldName     = "postCode"
  private val countryCodeFieldName  = "countryCode"

  val mapping: Mapping[Address] = Forms.mapping(
    addressLine1FieldName -> text()
      .verifying(emptyError(addressLine1FieldName), isNonEmpty)
      .verifying(notValidError(addressLine1FieldName), validateAddressField(addressFieldMaxSize)),
    addressLine2FieldName -> optional(
      text()
        .verifying(notValidError(addressLine2FieldName), validateAddressField(addressFieldMaxSize))
    ),
    addressLine3FieldName -> optional(
      text()
        .verifying(notValidError(addressLine3FieldName), validateAddressField(addressFieldMaxSize))
    ),
    townOrCityFieldName -> text()
      .verifying(emptyError(townOrCityFieldName), isNonEmpty)
      .verifying(notValidError(townOrCityFieldName), validateAddressField(addressFieldMaxSize)),
    postCodeFieldName -> mandatoryIfEqual(
      "countryCode",
      "GB",
      text()
        .transform[String](postCode => postCode.toUpperCase, postCode => postCode)
        .verifying(emptyError(postCodeFieldName), isNonEmpty)
        .verifying(notValidError(postCodeFieldName), validatePostcode(10))
    ),
    countryCodeFieldName -> text()
      .verifying(emptyError(countryCodeFieldName), isNonEmpty)
  )(Address.apply)(Address.unapply)

  def form(): Form[Address] = Form(mapping)

  def validateAsInput(address: Address): Form[Address] = Address.form().fillAndValidate(address)

  private def emptyError(field: String) = s"primaryContactDetails.address.$field.empty.error"

  private def notValidError(field: String) =
    s"primaryContactDetails.address.$field.format.error"

}
