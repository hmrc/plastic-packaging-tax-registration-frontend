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

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.Messages
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.CommonFormValidators
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipPartnerTypeEnum.PartnershipPartnerTypeEnum

object PartnershipPartnerTypeEnum extends Enumeration {
  type PartnershipPartnerTypeEnum = Value
  val SOLE_TRADER: Value                          = Value("SoleTrader")
  val UK_COMPANY: Value                           = Value("UkCompany")
  val LIMITED_LIABILITY_PARTNERSHIP: Value        = Value("LimitedLiabilityPartnership")
  val SCOTTISH_PARTNERSHIP: Value                 = Value("ScottishPartnership")
  val SCOTTISH_LIMITED_PARTNERSHIP: Value         = Value("ScottishLimitedPartnership")
  val CHARITABLE_INCORPORATED_ORGANISATION: Value = Value("CIO")
  val OVERSEAS_COMPANY_UK_BRANCH: Value           = Value("OverseasCompanyUkBranch")
  val OVERSEAS_COMPANY_NO_UK_BRANCH: Value        = Value("OverseasCompanyNoUKBranch")

  def withNameOpt(name: String): Option[Value] = values.find(_.toString == name)

  def displayName(
    partnershipPartnerTypeEnum: PartnershipPartnerTypeEnum
  )(implicit messages: Messages): String =
    messages(s"nominated.partner.type.$partnershipPartnerTypeEnum")

  implicit def value(partnershipPartnerTypeEnum: PartnershipPartnerTypeEnum): String =
    partnershipPartnerTypeEnum.toString

  implicit val format: Format[PartnershipPartnerTypeEnum] =
    Format(Reads.enumNameReads(PartnershipPartnerTypeEnum), Writes.enumNameWrites)

}

case class PartnershipPartnerType(answer: Option[PartnershipPartnerTypeEnum])

object PartnershipPartnerType extends CommonFormValidators {
  lazy val emptyError = "partnership.partner.name.empty.error"

  def form(): Form[PartnershipPartnerType] =
    Form(
      mapping(
        "answer" -> text()
          .verifying(emptyError, contains(PartnershipPartnerTypeEnum.values.toSeq.map(_.toString)))
      )(PartnershipPartnerType.apply)(PartnershipPartnerType.unapply)
    )

  def apply(value: String): PartnershipPartnerType =
    PartnershipPartnerType(PartnershipPartnerTypeEnum.withNameOpt(value))

  def unapply(partnershipPartnerType: PartnershipPartnerType): Option[String] =
    partnershipPartnerType.answer.map(_.toString)

}
