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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  PartnerTypeEnum,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}

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

  val partnershipOrCompanyAddress: Option[Address] = partnershipType match {
    case LIMITED_LIABILITY_PARTNERSHIP | LIMITED_PARTNERSHIP | SCOTTISH_LIMITED_PARTNERSHIP =>
      partnershipBusinessDetails.flatMap(_.companyAddress)
    case _ => None
  }

  def isGroupMemberSameAsNominatedPartnership(customerIdentification1: String): Boolean =
    partnershipBusinessDetails.exists(
      _.isGroupMemberSameAsNominatedPartnership(customerIdentification1)
    )

  def findPartner(partnerId: String): Option[Partner] =
    partners.find(_.id == partnerId)

  def withPromotedInflightPartner(): PartnershipDetails =
    this.copy(partners = partners :+ inflightPartner.getOrElse(
                throw new IllegalStateException("Inflight partner absent")
              ),
              inflightPartner = None
    )

}

object PartnershipDetails {
  implicit val format: OFormat[PartnershipDetails] = Json.format[PartnershipDetails]
}

case class PartnerPartnershipDetails(
  partnershipType: PartnerTypeEnum,
  partnershipName: Option[String] = None,
  partnershipBusinessDetails: Option[PartnershipBusinessDetails] = None
) {

  lazy val name: Option[String] = partnershipType match {
    case GENERAL_PARTNERSHIP | SCOTTISH_PARTNERSHIP =>
      if (partnershipName.isDefined)
        partnershipName
      else
        Some("TODO: Capture Non-Incorp Partnership Name") // TODO Confirm is this is still active
    case _ => partnershipBusinessDetails.flatMap(_.companyProfile.map(_.companyName))
  }

}

object PartnerPartnershipDetails {
  implicit val format: OFormat[PartnerPartnershipDetails] = Json.format[PartnerPartnershipDetails]
}
