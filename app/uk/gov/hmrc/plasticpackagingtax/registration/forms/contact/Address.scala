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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.contact

import play.api.data.Forms.{optional, text}
import play.api.data.{Form, Forms, Mapping}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.CommonFormValidators
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.AddressLookupConfirmation
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

case class Address(
  addressLine1: String,
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  townOrCity: String,
  postCode: Option[String],
  countryCode: String = "GB"
)

object Address extends CommonFormValidators {
  implicit val format: OFormat[Address] = Json.format[Address]

  def apply(addressLookupConfirmation: AddressLookupConfirmation): Address = {
    val lines = addressLookupConfirmation.extractAddressLines()
    new Address(addressLine1 = lines._1,
                addressLine2 = lines._2,
                addressLine3 = lines._3,
                townOrCity = lines._4,
                postCode = addressLookupConfirmation.address.postcode,
                countryCode = addressLookupConfirmation.address.country.map(_.code).getOrElse("GB")
    )
  }

  private val validateAddressField: Int => String => Boolean =
    (length: Int) =>
      (input: String) => isNotExceedingMaxLength(input, length) && isValidAddressInput(input)

  private val addressFieldMaxSize = 35

  val addressLine1 = "addressLine1"
  val addressLine2 = "addressLine2"
  val addressLine3 = "addressLine3"
  val townOrCity   = "townOrCity"
  val postCode     = "postCode"
  val countryCode  = "countryCode"

  val mapping: Mapping[Address] = Forms.mapping(
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
    postCode -> mandatoryIfEqual("countryCode",
                                 "GB",
                                 text()
                                   .transform[String](postCode => postCode.toUpperCase,
                                                      postCode => postCode
                                   )
                                   .verifying(emptyError(postCode), isNonEmpty)
                                   .verifying(notValidError(postCode), validatePostcode(10))
    ),
    countryCode -> text()
      .verifying(emptyError(countryCode), isNonEmpty)
  )(Address.apply)(Address.unapply)

  // Use this to unbind the form data from the request to get around the problem of conditional mandatoryIfEqual
  // mapping not binding postcode
  def dataExtractor(): Form[Address] =
    Form(
      Forms.mapping(addressLine1 -> Forms.text,
                    addressLine2 -> optional(Forms.text),
                    addressLine3 -> optional(Forms.text),
                    townOrCity   -> Forms.text,
                    postCode     -> optional(Forms.text),
                    countryCode  -> Forms.text
      )(Address.apply)(Address.unapply)
    )

  def form(): Form[Address] = Form(mapping)

  private def emptyError(field: String) = s"primaryContactDetails.address.$field.empty.error"

  private def notValidError(field: String) =
    s"primaryContactDetails.address.$field.format.error"

}
