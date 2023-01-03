/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.libs.json.{Json, OFormat}
import forms.CommonFormValidators

case class PhoneNumber(value: String)

object PhoneNumber extends CommonFormValidators {

  implicit val format: OFormat[PhoneNumber] = Json.format[PhoneNumber]

  lazy val phoneNumberEmptyError    = "primaryContactDetails.phoneNumber.empty.error"
  lazy val phoneNumberTooLongError  = "primaryContactDetails.phoneNumber.tooLong.error"
  lazy val phoneNumberInvalidFormat = "primaryContactDetails.phoneNumber.invalidFormat.error"
  val maxLength                     = 24
  val phoneNumber                   = "value"

  def form(): Form[PhoneNumber] =
    Form(
      mapping(
        phoneNumber -> text()
          .verifying(phoneNumberEmptyError, isNonEmpty)
          .verifying(phoneNumberTooLongError,
                     isNotExceedingMaxLengthExcludingWhitespaces(_, maxLength)
          )
          .verifying(phoneNumberInvalidFormat, isValidTelephoneNumber)
      )(PhoneNumber.apply)(PhoneNumber.unapply)
    )

}
