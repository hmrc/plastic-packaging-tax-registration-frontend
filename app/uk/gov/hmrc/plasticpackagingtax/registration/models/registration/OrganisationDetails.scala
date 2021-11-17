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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  OrgType,
  PARTNERSHIP,
  REGISTERED_SOCIETY,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationDetails,
  IncorporationRegistrationDetails,
  PartnershipDetails,
  SoleTraderIncorporationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.{
  SUBSCRIBED,
  Status
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

case class OrganisationDetails(
  organisationType: Option[OrgType] = None,
  businessRegisteredAddress: Option[Address] = None,
  soleTraderDetails: Option[SoleTraderIncorporationDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  partnershipDetails: Option[PartnershipDetails] = None,
  subscriptionStatus: Option[Status] = None
) {

  def status: TaskStatus =
    if (organisationType.isEmpty) TaskStatus.NotStarted
    else if (subscriptionCheckPassed && businessPartnerId.isDefined) TaskStatus.Completed
    else TaskStatus.InProgress

  lazy val subscriptionCheckPassed: Boolean = subscriptionStatus.exists(!_.equals(SUBSCRIBED))

  lazy val businessVerificationFailed: Boolean =
    incorporationDetails.exists(
      details =>
        details.registration.registrationStatus == "REGISTRATION_NOT_CALLED" && details.businessVerificationStatus == "FAIL"
    )

  lazy val businessPartnerId: Option[String] =
    organisationType match {
      case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) =>
        incorporationDetails.flatMap(details => details.registration.registeredBusinessPartnerId)
      case Some(SOLE_TRADER) =>
        soleTraderDetails.flatMap(details => details.registration.registeredBusinessPartnerId)
      case Some(PARTNERSHIP) =>
        partnershipDetails.flatMap { pd =>
          pd.partnershipType match {
            case GENERAL_PARTNERSHIP =>
              partnershipDetails.get.generalPartnershipDetails.get.registration.registeredBusinessPartnerId
            case SCOTTISH_PARTNERSHIP =>
              partnershipDetails.get.scottishPartnershipDetails.get.registration.registeredBusinessPartnerId
            case _ => None
          }
        }
      case _ => None
    }

  lazy val businessName: Option[String] = organisationType match {
    case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) => incorporationDetails.map(_.companyName)
    case Some(SOLE_TRADER)                           => soleTraderDetails.map(st => s"${st.firstName} ${st.lastName}")
    case _                                           => None
  }

  lazy val registrationDetails: Option[IncorporationRegistrationDetails] = organisationType match {
    case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) => incorporationDetails.map(_.registration)
    case Some(SOLE_TRADER)                           => soleTraderDetails.map(_.registration)
    case Some(PARTNERSHIP) =>
      partnershipDetails.flatMap(
        details =>
          details.generalPartnershipDetails.map(_.registration).orElse(
            details.scottishPartnershipDetails.map(_.registration)
          )
      )
    case _ => None
  }

}

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
