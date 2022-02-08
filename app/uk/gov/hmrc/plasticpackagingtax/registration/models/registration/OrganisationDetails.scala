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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  LIMITED_LIABILITY_PARTNERSHIP,
  PartnerTypeEnum,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.{
  SUBSCRIBED,
  Status
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.models.TaskStatus

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
          case PartnerTypeEnum.LIMITED_PARTNERSHIP | PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP |
              PartnerTypeEnum.SCOTTISH_PARTNERSHIP | PartnerTypeEnum.GENERAL_PARTNERSHIP =>
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

  lazy val nominatedPartner: Option[Partner] =
    partnershipDetails.flatMap(_.nominatedPartner)

  lazy val inflightPartner: Option[Partner] =
    partnershipDetails.flatMap(_.inflightPartner)

  def partnerType(partnerId: Option[String]): Option[PartnerTypeEnum] =
    partnerId match {
      case Some(partnerId) =>
        partnershipDetails.flatMap(_.findPartner(partnerId)).map(_.partnerType)
      case None => inflightPartner.map(_.partnerType)
    }

  def partnerGrsRegistration(partnerId: Option[String]): Option[RegistrationDetails] =
    partnerId match {
      case Some(partnerId) =>
        partnershipDetails.flatMap(_.findPartner(partnerId)) match {
          case Some(partner) => partnerGrsRegistrationDetails(partner)
          case _             => None
        }
      case _ =>
        inflightPartner match {
          case Some(partner) => partnerGrsRegistrationDetails(partner)
          case None          => None
        }
    }

  def partnerGrsRegistrationDetails(partner: Partner): Option[RegistrationDetails] =
    partner.partnerType match {
      case PartnerTypeEnum.SOLE_TRADER => partner.soleTraderDetails.flatMap(_.registration)
      case PartnerTypeEnum.UK_COMPANY | PartnerTypeEnum.OVERSEAS_COMPANY_UK_BRANCH |
          PartnerTypeEnum.REGISTERED_SOCIETY =>
        partner.incorporationDetails.flatMap(_.registration)
      case LIMITED_LIABILITY_PARTNERSHIP | SCOTTISH_PARTNERSHIP | SCOTTISH_LIMITED_PARTNERSHIP =>
        partner.partnerPartnershipDetails.flatMap(
          _.partnershipBusinessDetails.flatMap(_.registration)
        )
      case PartnerTypeEnum.CHARITABLE_INCORPORATED_ORGANISATION => None
      case PartnerTypeEnum.OVERSEAS_COMPANY_NO_UK_BRANCH        => None
      case _                                                    => None
    }

  def partnerRegistrationStatus(partnerId: Option[String]): Option[String] =
    partnerGrsRegistration(partnerId).map { reg =>
      reg.registrationStatus
    }

  def partnerBusinessVerificationFailed(partnerId: Option[String]): Boolean =
    partnerGrsRegistration(partnerId).map(
      _.registrationStatus.contains("REGISTRATION_NOT_CALLED")
    ).get && partnerVerificationStatus(partnerId).contains("FAIL")

  def partnerVerificationStatus(partnerId: Option[String]): Option[String] =
    partnerGrsRegistration(partnerId).flatMap { reg =>
      reg.verificationStatus
    }

  def partnerBusinessPartnerId(partnerId: Option[String]): Option[String] =
    partnerGrsRegistration(partnerId).flatMap { reg =>
      reg.registeredBusinessPartnerId
    }

}

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}
