/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.{Format, Reads, Writes}
import forms.CommonFormValidators
import forms.organisation.OrgType.OrgType

object OrgType extends Enumeration {
  type OrgType = Value
  val UK_COMPANY: Value                           = Value("UkCompany")
  val SOLE_TRADER: Value                          = Value("SoleTrader")
  val PARTNERSHIP: Value                          = Value("Partnership")
  val REGISTERED_SOCIETY: Value                   = Value("RegisteredSociety")
  val TRUST: Value                                = Value("Trust")
  val CHARITABLE_INCORPORATED_ORGANISATION: Value = Value("CIO")
  val OVERSEAS_COMPANY_UK_BRANCH: Value           = Value("OverseasCompanyUkBranch")
  val OVERSEAS_COMPANY_NO_UK_BRANCH: Value        = Value("OverseasCompanyNoUKBranch")

  def withNameOpt(name: String): Option[Value] = values.find(_.toString == name)

  def displayName(orgType: OrgType)(implicit messages: Messages): String =
    messages(s"organisationDetails.type.$orgType")

  implicit def value(orgType: OrgType): String = orgType.toString

  implicit val format: Format[OrgType] =
    Format(Reads.enumNameReads(OrgType), Writes.enumNameWrites)

}

object ActionEnum extends Enumeration {
  type Type = Value

  val Org, RepresentativeMember, Group = Value
}

case class OrganisationType(answer: Option[OrgType])

object OrganisationType extends CommonFormValidators {

  def form(action: ActionEnum.Type): Form[OrganisationType] = {
    val emptyError = action match {
      case ActionEnum.Org                  => "organisationDetails.type.empty.error"
      case ActionEnum.RepresentativeMember => "organisationDetails.type.empty.member.error"
      case ActionEnum.Group                => "organisationDetails.type.empty.group.error"
      case other                           => throw new IllegalStateException(s"Invalid action: $other")
    }

    Form(
      mapping(
        "answer" -> nonEmptyString(emptyError)
          .verifying(emptyError, contains(OrgType.values.toSeq.map(_.toString)))
      )(OrganisationType.apply)(OrganisationType.unapply)
    )
  }

  def apply(value: String): OrganisationType = OrganisationType(OrgType.withNameOpt(value))

  def unapply(organisationType: OrganisationType): Option[String] =
    organisationType.answer.map(_.toString)

}
