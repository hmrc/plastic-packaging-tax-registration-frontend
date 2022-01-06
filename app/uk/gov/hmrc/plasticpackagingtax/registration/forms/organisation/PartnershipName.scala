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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation

import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address.{
  isMatchingPattern,
  isNonEmpty
}

import java.util.regex.Pattern

case class PartnershipName(value: String)

object PartnershipName {

  implicit val format: OFormat[PartnershipName] = Json.format[PartnershipName]
  val partnershipNameEmptyError                 = "partnership.name.empty.error"
  val partnershipNameFormatError                = "partnership.name.format.error"

  private val PARTNERSHIP_NAME_REGEX =
    Pattern.compile("""^[a-zA-Z0-9À-ÿ !#$%&'‘’"“”«»()*+,./:;=?@\[\]£€¥\\—–‐-]{1,160}$""")

  val maxLength       = 160
  val partnershipName = "value"

  private val mapping = Forms.mapping(
    partnershipName ->
      text()
        .verifying(partnershipNameEmptyError, isNonEmpty)
        .verifying(partnershipNameFormatError,
                   partnershipName =>
                     !isNonEmpty(partnershipName) || isMatchingPattern(partnershipName,
                                                                       PARTNERSHIP_NAME_REGEX
                     )
        )
  )(PartnershipName.apply)(PartnershipName.unapply)

  def form(): Form[PartnershipName] = Form(mapping)
}
