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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.Messages
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.CommonFormValidators
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.PartnershipTypeEnum

object PartnershipTypeEnum extends Enumeration {
  type PartnershipTypeEnum = Value
  val GENERAL_PARTNERSHIP: Value           = Value("GeneralPartnership")
  val LIMITED_LIABILITY_PARTNERSHIP: Value = Value("LimitedLiabilityPartnership")
  val LIMITED_PARTNERSHIP: Value           = Value("LimitedPartnership")
  val SCOTTISH_PARTNERSHIP: Value          = Value("ScottishPartnership")
  val SCOTTISH_LIMITED_PARTNERSHIP: Value  = Value("ScottishLimitedPartnership")

  def withNameOpt(name: String): Option[Value] = values.find(_.toString == name)

  def displayName(partnershipTypeEnum: PartnershipTypeEnum)(implicit messages: Messages): String =
    messages(s"partnership.type.$partnershipTypeEnum")

  implicit def value(partnershipTypeEnum: PartnershipTypeEnum): String =
    partnershipTypeEnum.toString

  implicit val format: Format[PartnershipTypeEnum] =
    Format(Reads.enumNameReads(PartnershipTypeEnum), Writes.enumNameWrites)

}

case class PartnershipType(answer: Option[PartnershipTypeEnum])

object PartnershipType extends CommonFormValidators {
  lazy val emptyError = "partnership.type.empty.error"

  def form(): Form[PartnershipType] =
    Form(
      mapping(
        "answer" -> text()
          .verifying(emptyError, contains(PartnershipTypeEnum.values.toSeq.map(_.toString)))
      )(PartnershipType.apply)(PartnershipType.unapply)
    )

  def apply(value: String): PartnershipType =
    PartnershipType(PartnershipTypeEnum.withNameOpt(value))

  def unapply(partnershipType: PartnershipType): Option[String] =
    partnershipType.answer.map(_.toString)

}
