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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.PartnerTypeEnum

object PartnerTypeEnum extends Enumeration {
  type PartnerTypeEnum = Value
  val SOLE_TRADER: Value                          = Value("SoleTrader")
  val UK_COMPANY: Value                           = Value("UkCompany")
  val REGISTERED_SOCIETY: Value                   = Value("RegisteredSociety")
  val GENERAL_PARTNERSHIP: Value                  = Value("GeneralPartnership")
  val LIMITED_LIABILITY_PARTNERSHIP: Value        = Value("LimitedLiabilityPartnership")
  val LIMITED_PARTNERSHIP: Value                  = Value("LimitedPartnership")
  val SCOTTISH_PARTNERSHIP: Value                 = Value("ScottishPartnership")
  val SCOTTISH_LIMITED_PARTNERSHIP: Value         = Value("ScottishLimitedPartnership")
  val CHARITABLE_INCORPORATED_ORGANISATION: Value = Value("CIO")
  val OVERSEAS_COMPANY_UK_BRANCH: Value           = Value("OverseasCompanyUkBranch")
  val OVERSEAS_COMPANY_NO_UK_BRANCH: Value        = Value("OverseasCompanyNoUKBranch")

  def withNameOpt(name: String): Option[Value] = values.find(_.toString == name)

  def displayName(partnerTypeEnum: PartnerTypeEnum)(implicit messages: Messages): String =
    messages(s"partner.type.$partnerTypeEnum")

  implicit def value(partnerTypeEnum: PartnerTypeEnum): String =
    partnerTypeEnum.toString

  implicit val format: Format[PartnerTypeEnum] =
    Format(Reads.enumNameReads(PartnerTypeEnum), Writes.enumNameWrites)

}

case class PartnerType(answer: PartnerTypeEnum)

object PartnerType extends CommonFormValidators {
  lazy val emptyError = "partnership.partner.name.empty.error"

  def form(): Form[PartnerType] =
    Form(
      mapping(
        "answer" -> text()
          .verifying(emptyError, contains(PartnerTypeEnum.values.toSeq.map(_.toString)))
      )(PartnerType.apply)(PartnerType.unapply)
    )

  def apply(value: String): PartnerType =
    PartnerType(PartnerTypeEnum.withName(value))

  def unapply(partnerType: PartnerType): Option[String] =
    Some(partnerType.answer.toString)

}
