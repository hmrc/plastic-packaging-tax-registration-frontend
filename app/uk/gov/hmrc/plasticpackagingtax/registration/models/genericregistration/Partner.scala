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

package uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.PartnerTypeEnum

import java.util.UUID

case class Partner(
  id: String = UUID.randomUUID().toString,
  partnerType: Option[PartnerTypeEnum],
  soleTraderDetails: Option[SoleTraderDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  partnerPartnershipDetails: Option[PartnerPartnershipDetails] = None,
  contactDetails: Option[PartnerContactDetails] = None
) {

  lazy val name: String = partnerType match {
    case Some(PartnerTypeEnum.SOLE_TRADER) =>
      soleTraderDetails.map(_.name).getOrElse(
        throw new IllegalStateException("Sole Trader details name absent")
      )
    case Some(PartnerTypeEnum.SCOTTISH_PARTNERSHIP) | Some(PartnerTypeEnum.GENERAL_PARTNERSHIP) |
        Some(PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP) | Some(
          PartnerTypeEnum.SCOTTISH_LIMITED_PARTNERSHIP
        ) =>
      partnerPartnershipDetails.flatMap(_.name).getOrElse(
        throw new IllegalStateException("Partnership details name absent")
      )
    case _ =>
      incorporationDetails.map(_.companyName).getOrElse(
        throw new IllegalStateException("Incorporation details name absent")
      )
  }

  def withContactAddress(contactAddress: Address): Partner =
    this.copy(contactDetails = this.contactDetails.map(_.withUpdatedAddress(contactAddress)))

  def canEditName: Boolean = {
    val partnerTypesWhichPermitUserSuppliedNames =
      Set(PartnerTypeEnum.SCOTTISH_PARTNERSHIP, PartnerTypeEnum.GENERAL_PARTNERSHIP)
    partnerType.exists { partnerType =>
      partnerTypesWhichPermitUserSuppliedNames.contains(partnerType)
    }
  }

}

object Partner {
  implicit val format: OFormat[Partner] = Json.format[Partner]
}
