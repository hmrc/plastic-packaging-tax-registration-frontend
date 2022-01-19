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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  OVERSEAS_COMPANY_NO_UK_BRANCH,
  PartnerTypeEnum,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{
  PartnerTypeEnum,
  PartnershipTypeEnum
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.{
  LIMITED_LIABILITY_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorporationDetails,
  Partner,
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
  regWithoutIDFlag: Option[Boolean] = None,
  isBusinessAddressFromGrs: Option[Boolean] = None
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
    case Some(PARTNERSHIP) =>
      partnershipDetails.flatMap(_.partnershipOrCompanyName)
    case _ => None
  }

  def withBusinessRegisteredAddress(): OrganisationDetails = {
    val businessAddress = organisationType match {
      case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
        incorporationDetails.map(
          incorporationDetails => incorporationDetails.companyAddress.toPptAddress
        )
      case _ => None
    }
    this.copy(businessRegisteredAddress = businessAddress,
              isBusinessAddressFromGrs = Some(businessAddress.isDefined)
    )
  }

  lazy val nominatedPartner: Option[Partner] = partnershipDetails.map(_.nominatedPartner).getOrElse(
    throw new IllegalStateException("No partnership details found")
  )

  lazy val nominatedPartnerType: Option[PartnerTypeEnum] = nominatedPartner.flatMap(_.partnerType)

  lazy val nominatedPartnerGrsRegistration: Option[RegistrationDetails] = nominatedPartner match {
    case Some(partner) =>
      partner.partnerType match {
        case Some(partnerType) =>
          partnerType match {
            case PartnerTypeEnum.SOLE_TRADER => partner.soleTraderDetails.flatMap(_.registration)
            case PartnerTypeEnum.UK_COMPANY | PartnerTypeEnum.OVERSEAS_COMPANY_UK_BRANCH =>
              partner.incorporationDetails.flatMap(_.registration)
            case PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP        => None
            case PartnerTypeEnum.SCOTTISH_PARTNERSHIP                 => None
            case PartnerTypeEnum.SCOTTISH_LIMITED_PARTNERSHIP         => None
            case PartnerTypeEnum.CHARITABLE_INCORPORATED_ORGANISATION => None
            case PartnerTypeEnum.OVERSEAS_COMPANY_NO_UK_BRANCH        => None
          }
        case _ => None
      }
    case _ => None
  }

  lazy val nominatedPartnerRegistrationStatus: Option[String] =
    nominatedPartnerGrsRegistration.map { reg =>
      reg.registrationStatus
    }

  lazy val nominatedPartnerBusinessVerificationFailed: Boolean =
    nominatedPartnerRegistrationStatus.contains(
      "REGISTRATION_NOT_CALLED"
    ) && nominatedPartnerVerificationStatus.contains("FAIL")

  lazy val nominatedPartnerVerificationStatus: Option[String] =
    nominatedPartnerGrsRegistration.flatMap { reg =>
      reg.verificationStatus
    }

  lazy val nominatedPartnerBusinessPartnerId: Option[String] =
    nominatedPartnerGrsRegistration.flatMap { reg =>
      reg.registeredBusinessPartnerId
    }

}

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
