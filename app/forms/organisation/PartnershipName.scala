/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.organisation

import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}
import forms.contact.Address.{isMatchingPattern, isNonEmpty}

import java.util.regex.Pattern

case class PartnershipName(value: String)

object PartnershipName {

  implicit val format: OFormat[PartnershipName] = Json.format[PartnershipName]
  private val PARTNERSHIP_NAME_REGEX =
    Pattern.compile("""^[a-zA-Z0-9À-ÿ !#$%&'‘’"“”«»()*+,./:;=?@\[\]£€¥\\—–‐-]{1,160}$""")
  private val maxLength       = 160
  private val partnershipName = "value"

  private val mapping = Forms.mapping(
    partnershipName ->
      text()
        .verifying("partnership.name.empty.error", isNonEmpty)
        .verifying("partnership.name.too-long", isShortEnough).verifying()
        .verifying("partnership.name.format.error", matchesRegex)
  )(PartnershipName.apply)(PartnershipName.unapply)

  private def isShortEnough: String => Boolean = { name =>
    name.length <= maxLength
  }

  private def matchesRegex: String => Boolean = { partnershipName =>
    !isNonEmpty(partnershipName) || isMatchingPattern(partnershipName, PARTNERSHIP_NAME_REGEX)
  }

  def form(): Form[PartnershipName] = Form(mapping)
}
