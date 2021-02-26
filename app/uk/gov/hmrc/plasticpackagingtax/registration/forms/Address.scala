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

package uk.gov.hmrc.plasticpackagingtax.registration.forms

import play.api.data.Forms.{optional, text}
import play.api.data.{Form, Forms, Mapping}
import play.api.libs.json.{Json, OFormat}

case class Address(
  businessName: Option[String] = None,
  addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  townOrCity: String,
  county: Option[String] = None,
  postCode: String
)

object Address extends CommonFormValidators {
  implicit val format: OFormat[Address] = Json.format[Address]

  private val validateAddressField: Int => String => Boolean =
    (length: Int) =>
      (input: String) => isNotExceedingMaxLength(input, length) && isValidAddressInput(input)

  private val validatePostcode: Int => String => Boolean =
    (length: Int) =>
      (input: String) => isNotExceedingMaxLength(input, length) && isValidPostcode(input)

  private val addressFieldMaxSize = 35

  val businessName = "businessName"
  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val townOrCity   = "townOrCity"
  val county       = "county"
  val postCode     = "postCode"

  val mapping: Mapping[Address] = Forms.mapping(
    businessName -> optional(
      text()
        .verifying(notValidError(businessName), validateAddressField(addressFieldMaxSize))
    ),
    addressLine1 -> text()
      .verifying(emptyError(addressLine1), isNonEmpty)
      .verifying(notValidError(addressLine1), validateAddressField(addressFieldMaxSize)),
    addressLine2 -> optional(
      text()
        .verifying(notValidError(addressLine2), validateAddressField(addressFieldMaxSize))
    ),
    addressLine3 -> optional(
      text()
        .verifying(notValidError(addressLine3), validateAddressField(addressFieldMaxSize))
    ),
    townOrCity -> text()
      .verifying(emptyError(townOrCity), isNonEmpty)
      .verifying(notValidError(townOrCity), validateAddressField(addressFieldMaxSize)),
    county -> optional(
      text()
        .verifying(notValidError(county), validateAddressField(addressFieldMaxSize))
    ),
    postCode -> text()
      .verifying(emptyError(postCode), isNonEmpty)
      .verifying(notValidError(postCode), validatePostcode(10))
  )(Address.apply)(Address.unapply)

  def form(): Form[Address] = Form(mapping)

  private def emptyError(field: String) = s"primaryContactDetails.address.$field.empty.error"

  private def notValidError(field: String) =
    s"primaryContactDetails.address.$field.format.error"

}
