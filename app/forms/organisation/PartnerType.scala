/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.i18n.Messages
import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}
import forms.CommonFormValidators
import forms.organisation.PartnerTypeEnum.given_Format_PartnerTypeEnum

import scala.util.Try

enum PartnerTypeEnum(val value: String):
  case SOLE_TRADER                          extends PartnerTypeEnum("SoleTrader")
  case UK_COMPANY                           extends PartnerTypeEnum("UkCompany")
  case REGISTERED_SOCIETY                   extends PartnerTypeEnum("RegisteredSociety")
  case GENERAL_PARTNERSHIP                  extends PartnerTypeEnum("GeneralPartnership")
  case LIMITED_LIABILITY_PARTNERSHIP        extends PartnerTypeEnum("LimitedLiabilityPartnership")
  case LIMITED_PARTNERSHIP                  extends PartnerTypeEnum("LimitedPartnership")
  case SCOTTISH_PARTNERSHIP                 extends PartnerTypeEnum("ScottishPartnership")
  case SCOTTISH_LIMITED_PARTNERSHIP         extends PartnerTypeEnum("ScottishLimitedPartnership")
  case CHARITABLE_INCORPORATED_ORGANISATION extends PartnerTypeEnum("CIO")
  case OVERSEAS_COMPANY_UK_BRANCH           extends PartnerTypeEnum("OverseasCompanyUkBranch")
  case OVERSEAS_COMPANY_NO_UK_BRANCH        extends PartnerTypeEnum("OverseasCompanyNoUKBranch")

  override def toString: String = value // for backwards compatibility only

object PartnerTypeEnum {
  def withNameOpt(name: String): Option[PartnerTypeEnum] = PartnerTypeEnum.values.find(_.value == name)

  def withName(name: String): PartnerTypeEnum =
    PartnerTypeEnum.values.find(_.value == name)
      .getOrElse(throw new NoSuchElementException(s"unable to find partner type: $name"))

  def displayName(partnerTypeEnum: PartnerTypeEnum)(implicit messages: Messages): String =
    messages(s"partner.type.${partnerTypeEnum.value}")

  implicit def value(partnerTypeEnum: PartnerTypeEnum): String =
    partnerTypeEnum.value

  given Format[PartnerTypeEnum] =
    Format(
      Reads {
        case JsString(value) =>
          PartnerTypeEnum.values.find(_.value == value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Unknown PartnerTypeEnum: $value"))
        case _ =>
          JsError("String value expected")
      },
      Writes(partnerTypeEnum => JsString(partnerTypeEnum.value))
    )
}

case class PartnerType(answer: PartnerTypeEnum)

object PartnerType extends CommonFormValidators {

  sealed trait FormMode

  object FormMode {
    case object PartnershipType      extends FormMode
    case object NominatedPartnerType extends FormMode
    case object OtherPartnerType     extends FormMode
  }

  private def emptyError(mode: FormMode): String =
    mode match {
      case FormMode.NominatedPartnerType => "partnership.nominated.type.empty.error"
      case FormMode.OtherPartnerType     => "partnership.other.type.empty.error"
      case FormMode.PartnershipType      => "partnership.type.empty.error"
    }

  def form(mode: FormMode): Form[PartnerType] =
    Form(
      mapping(
        "answer" -> nonEmptyString(emptyError(mode))
          .verifying(emptyError(mode), contains(PartnerTypeEnum.values.toSeq.map(_.value)))
          .transform[PartnerTypeEnum](str => PartnerTypeEnum.withName(str), _.value)
      )(PartnerType.apply)(pt => Some(pt.answer))
    )

}
