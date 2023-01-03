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

package forms.partner

import play.api.data.{Form, Forms}
import play.api.data.Forms.text
import play.api.libs.json.{Json, OFormat}
import forms.contact.Address.{
  isMatchingPattern,
  isNonEmpty
}

import java.util.regex.Pattern

case class PartnerName(value: String)

object PartnerName {

  implicit val format: OFormat[PartnerName] = Json.format[PartnerName]
  val partnerNameEmptyError                 = "partnership.name.empty.error"
  val partnerNameFormatError                = "partnership.name.format.error"

  private val PARTNER_NAME_REGEX =
    Pattern.compile("""^[a-zA-Z0-9À-ÿ !#$%&'‘’"“”«»()*+,./:;=?@\[\]£€¥\\—–‐-]{1,160}$""")

  val maxLength   = 160
  val partnerName = "value"

  private val mapping = Forms.mapping(
    partnerName ->
      text()
        .verifying(partnerNameEmptyError, isNonEmpty)
        .verifying(partnerNameFormatError,
                   partnerName =>
                     !isNonEmpty(partnerName) || isMatchingPattern(partnerName, PARTNER_NAME_REGEX)
        )
  )(PartnerName.apply)(PartnerName.unapply)

  def form(): Form[PartnerName] = Form(mapping)
}
