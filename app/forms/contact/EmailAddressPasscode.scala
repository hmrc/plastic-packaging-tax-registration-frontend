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

import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}
import forms.contact.Address.isNonEmpty

case class EmailAddressPasscode(value: String)

object EmailAddressPasscode {

  implicit val format: OFormat[EmailAddressPasscode] = Json.format[EmailAddressPasscode]

  lazy val emailAddressPasscodeEmptyError =
    "primaryContactDetails.emailAddress.passcode.empty.error"

  val maxLength            = 241
  val emailAddressPasscode = "value"

  private val mapping = Forms.mapping(
    emailAddressPasscode ->
      text()
        .verifying(emailAddressPasscodeEmptyError, isNonEmpty)
  )(EmailAddressPasscode.apply)(EmailAddressPasscode.unapply)

  def form(): Form[EmailAddressPasscode] = Form(mapping)
}
