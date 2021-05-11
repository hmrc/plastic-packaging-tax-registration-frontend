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

import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Address.isValidEmail
import uk.gov.hmrc.plasticpackagingtax.registration.forms.FullName.isNonEmpty
import uk.gov.hmrc.plasticpackagingtax.registration.forms.JobTitle.isNotExceedingMaxLength

case class EmailAddress(value: String)

object EmailAddress {

  implicit val format: OFormat[EmailAddress] = Json.format[EmailAddress]
  lazy val emailAddressEmptyError            = "primaryContactDetails.emailAddress.empty.error"
  lazy val emailAddressFormatError           = "primaryContactDetails.emailAddress.format.error"

  val maxLength    = 241
  val emailAddress = "value"

  private val mapping = Forms.mapping(
    emailAddress ->
      text()
        .verifying(emailAddressEmptyError, isNonEmpty)
        .verifying(emailAddressFormatError,
                   email => isNotExceedingMaxLength(email, maxLength) && isValidEmail(email)
        )
  )(EmailAddress.apply)(EmailAddress.unapply)

  def form(): Form[EmailAddress] = Form(mapping)
}
