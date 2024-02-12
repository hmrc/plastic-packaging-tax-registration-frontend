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

package models.genericregistration

import play.api.libs.json.{Json, OFormat}
import forms.contact.Address
import forms.organisation.PartnerTypeEnum
import forms.organisation.PartnerTypeEnum.PartnerTypeEnum

import java.util.UUID

case class Partner(
  id: String = UUID.randomUUID().toString,
  partnerType: PartnerTypeEnum,
  soleTraderDetails: Option[SoleTraderDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  partnerPartnershipDetails: Option[PartnerPartnershipDetails] = None,
  contactDetails: Option[PartnerContactDetails] = None
) {

  def registrationDetails: Option[RegistrationDetails] = {
    partnerType match {
      case PartnerTypeEnum.SOLE_TRADER => soleTraderRegistration
      case PartnerTypeEnum.SCOTTISH_PARTNERSHIP
           | PartnerTypeEnum.GENERAL_PARTNERSHIP
           | PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
           | PartnerTypeEnum.SCOTTISH_LIMITED_PARTNERSHIP => partnerRegistration
      case _ => incorporationRegistration
    }
  }

  private def soleTraderRegistration = soleTraderDetails.flatMap(_.registration)

  private def partnerRegistration =
    partnerPartnershipDetails.flatMap(_.partnershipBusinessDetails.flatMap(_.registration))

  private def incorporationRegistration = incorporationDetails.flatMap(_.registration)

  def name: String = {
    partnerType match {
      case PartnerTypeEnum.SOLE_TRADER => soleTraderName
      case PartnerTypeEnum.SCOTTISH_PARTNERSHIP
           | PartnerTypeEnum.GENERAL_PARTNERSHIP
           | PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
           | PartnerTypeEnum.SCOTTISH_LIMITED_PARTNERSHIP => partnerName
      case _ => incorporationName
    }
  }

  private def incorporationName = {
    incorporationDetails.map(_.companyName).getOrElse(
      throw new IllegalStateException("Incorporation details name absent")
    )
  }

  private def partnerName = {
    partnerPartnershipDetails.flatMap(_.name).getOrElse(
      throw new IllegalStateException("Partnership details name absent")
    )
  }

  private def soleTraderName = {
    soleTraderDetails.map(_.name).getOrElse(
      throw new IllegalStateException("Sole Trader details name absent")
    )
  }

  def withContactAddress(contactAddress: Option[Address]): Partner =
    this.copy(contactDetails = this.contactDetails.map(_.withUpdatedAddress(contactAddress)))

  def canEditName: Boolean = {
    val partnerTypesWhichPermitUserSuppliedNames =
      Set(PartnerTypeEnum.SCOTTISH_PARTNERSHIP, PartnerTypeEnum.GENERAL_PARTNERSHIP)
    partnerTypesWhichPermitUserSuppliedNames.contains(partnerType)
  }

}

object Partner {
  implicit val format: OFormat[Partner] = Json.format[Partner]
}
