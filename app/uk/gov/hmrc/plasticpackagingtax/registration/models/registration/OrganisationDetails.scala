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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  OrgType,
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationDetails,
  PartnershipDetails,
  SoleTraderIncorporationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

case class OrganisationDetails(
  isBasedInUk: Option[Boolean] = None,
  organisationType: Option[OrgType] = None,
  businessRegisteredAddress: Option[Address] = None,
  safeNumber: Option[String] = None,
  soleTraderDetails: Option[SoleTraderIncorporationDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  partnershipDetails: Option[PartnershipDetails] = None
) {

  def status: TaskStatus =
    if (isBasedInUk.isEmpty) TaskStatus.NotStarted
    else if (businessPartnerIdPresent()) TaskStatus.Completed
    else TaskStatus.InProgress

  val businessVerificationFailed: Boolean =
    incorporationDetails.exists(
      details =>
        details.registration.registrationStatus == "REGISTRATION_NOT_CALLED" && details.businessVerificationStatus == "FAIL"
    )

  def businessPartnerIdPresent(): Boolean =
    organisationType match {
      case Some(UK_COMPANY) =>
        incorporationDetails.exists(_.registration.registeredBusinessPartnerId.isDefined)
      case Some(SOLE_TRADER) =>
        soleTraderDetails.exists(_.registration.registeredBusinessPartnerId.isDefined)
      case Some(PARTNERSHIP) =>
        partnershipDetails.exists(_.partnershipType match {
          case GENERAL_PARTNERSHIP =>
            partnershipDetails.get.generalPartnershipDetails.get.registration.registeredBusinessPartnerId.isDefined
          case SCOTTISH_PARTNERSHIP =>
            partnershipDetails.get.scottishPartnershipDetails.get.registration.registeredBusinessPartnerId.isDefined
          case _ => false
        })
      case _ => false
    }

}

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
