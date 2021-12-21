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
  OVERSEAS_COMPANY_UK_BRANCH,
  OrgType,
  PARTNERSHIP,
  REGISTERED_SOCIETY,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationDetails,
  PartnershipDetails,
  RegistrationDetails,
  SoleTraderDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.{
  SUBSCRIBED,
  Status
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

case class OrganisationDetails(
  organisationType: Option[OrgType] = None,
  businessRegisteredAddress: Option[Address] = None,
  soleTraderDetails: Option[SoleTraderDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  partnershipDetails: Option[PartnershipDetails] = None,
  subscriptionStatus: Option[Status] = None,
  regWithoutIDFlag: Option[Boolean] = None
) {

  def status: TaskStatus =
    if (organisationType.isEmpty) TaskStatus.NotStarted
    else if (subscriptionCheckPassed && businessPartnerId.isDefined) TaskStatus.Completed
    else TaskStatus.InProgress

  lazy val subscriptionCheckPassed: Boolean = subscriptionStatus.exists(!_.equals(SUBSCRIBED))

  lazy val grsRegistration: Option[RegistrationDetails] = organisationType match {
    case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
      incorporationDetails.flatMap(orgDetails => orgDetails.registration)
    case Some(SOLE_TRADER) => soleTraderDetails.flatMap(orgDetails => orgDetails.registration)
    case Some(PARTNERSHIP) =>
      partnershipDetails.flatMap { partnershipDetails =>
        partnershipDetails.partnershipType match {
          case PartnershipTypeEnum.LIMITED_PARTNERSHIP |
              PartnershipTypeEnum.LIMITED_LIABILITY_PARTNERSHIP |
              PartnershipTypeEnum.SCOTTISH_PARTNERSHIP | PartnershipTypeEnum.GENERAL_PARTNERSHIP =>
            partnershipDetails.partnershipBusinessDetails.flatMap { orgDetails =>
              orgDetails.registration
            }
          case _ => None
        }
      }
    case _ => None
  }

  lazy val identifiersMatch: Option[Boolean] = grsRegistration.map(reg => reg.identifiersMatch)

  lazy val verificationStatus: Option[String] = grsRegistration.flatMap { reg =>
    reg.verificationStatus
  }

  lazy val registrationStatus: Option[String] = grsRegistration.map { reg =>
    reg.registrationStatus
  }

  lazy val businessPartnerId: Option[String] = grsRegistration.flatMap { reg =>
    reg.registeredBusinessPartnerId
  }

  lazy val businessVerificationFailed: Boolean =
    registrationStatus.contains("REGISTRATION_NOT_CALLED") && verificationStatus.contains("FAIL")

  lazy val businessName: Option[String] = organisationType match {
    case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
      incorporationDetails.map(_.companyName)
    case Some(SOLE_TRADER) => soleTraderDetails.map(st => s"${st.firstName} ${st.lastName}")
    case _                 => None
  }

  def withBusinessRegisteredAddress(): OrganisationDetails = {
    val businessAddress = organisationType match {
      case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
        incorporationDetails.map(
          incorporationDetails => incorporationDetails.companyAddress.toPptAddress
        )
      case Some(SOLE_TRADER) =>
        // TODO : temporary while we're working out where to get it from
        Some(
          Address(addressLine1 = "2 Scala Street",
                  addressLine2 = Some("Soho"),
                  townOrCity = "London",
                  postCode = Some("W1T 2HN")
          )
        )
      case Some(PARTNERSHIP) =>
        // TODO : temporary while we're working out where to get it from
        Some(
          Address(addressLine1 = "3 Scala Street",
                  addressLine2 = Some("Soho"),
                  townOrCity = "London",
                  postCode = partnershipDetails.flatMap(
                    pd => pd.partnershipBusinessDetails.map(pbd => pbd.postcode)
                  )
          )
        )
      case _ => None
    }
    this.copy(businessRegisteredAddress = businessAddress)
  }

}

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
