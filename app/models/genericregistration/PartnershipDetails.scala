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

package models.genericregistration

import play.api.libs.json.{Json, OFormat}
import forms.contact.Address
import forms.organisation.PartnerTypeEnum.{LIMITED_LIABILITY_PARTNERSHIP, LIMITED_PARTNERSHIP, PartnerTypeEnum, SCOTTISH_LIMITED_PARTNERSHIP}
import utils.AddressConversionUtils

case class PartnershipDetails(
  partnershipType: PartnerTypeEnum,
  partnershipName: Option[String] = None,
  partnershipBusinessDetails: Option[PartnershipBusinessDetails] = None,
  partners: Seq[Partner] = Seq(),
  inflightPartner: Option[Partner] = None // Scratch area for newly added partner
) {

  val nominatedPartner: Option[Partner] = partners.headOption
  val otherPartners: Seq[Partner]       = partners.drop(1)
  val newPartner: Option[Partner]       = partners.lastOption

  val partnershipOrCompanyName: Option[String] = partnershipName match {
    case Some(name) => Some(name)
    case _          => partnershipBusinessDetails.flatMap(_.companyName)
  }

  def isNominatedPartner(partnerId: Option[String]): Boolean =
    partnerId match {
      case Some(partnerId)               => nominatedPartner.exists(_.id.equals(partnerId))
      case _ if nominatedPartner.isEmpty => true
      case _                             => false
    }

  def partnershipOrCompanyAddress(addressConversionUtils: AddressConversionUtils): Option[Address] =
    partnershipType match {
      case LIMITED_LIABILITY_PARTNERSHIP | LIMITED_PARTNERSHIP | SCOTTISH_LIMITED_PARTNERSHIP =>
        partnershipBusinessDetails.flatMap(_.companyAddress(addressConversionUtils))
      case _ => None
    }

  def isGroupMemberSameAsNominatedPartnership(customerIdentification1: String): Boolean =
    partnershipBusinessDetails.exists(_.isGroupMemberSameAsNominatedPartnership(customerIdentification1))

  def findPartner(partnerId: String): Option[Partner] =
    partners.find(_.id == partnerId)

  def withPromotedInflightPartner(): PartnershipDetails =
    this.copy(
      partners = partners :+ inflightPartner.getOrElse(throw new IllegalStateException("Inflight partner absent")),
      inflightPartner = None
    )

}

object PartnershipDetails {
  implicit val format: OFormat[PartnershipDetails] = Json.format[PartnershipDetails]
}

case class PartnerPartnershipDetails(
  partnershipName: Option[String] = None,
  partnershipBusinessDetails: Option[PartnershipBusinessDetails] = None
) {

  def name: Option[String] =
    Seq(partnershipName, partnershipBusinessDetails.flatMap(_.companyProfile.map(_.companyName))).flatten.headOption

}

object PartnerPartnershipDetails {
  implicit val format: OFormat[PartnerPartnershipDetails] = Json.format[PartnerPartnershipDetails]
}
