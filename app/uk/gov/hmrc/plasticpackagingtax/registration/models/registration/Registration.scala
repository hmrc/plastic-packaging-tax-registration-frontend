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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType.{GROUP, RegType}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{OrgType, PartnerTypeEnum}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMember
import uk.gov.hmrc.plasticpackagingtax.registration.utils.AddressConversionUtils
import uk.gov.hmrc.plasticpackagingtax.registration.views.models.TaskStatus

import java.time.LocalDate

case class Registration(
                         id: String,
                         dateOfRegistration: Option[LocalDate] = Some(LocalDate.now()),
                         incorpJourneyId: Option[String] = None,
                         registrationType: Option[RegType] = None,
                         groupDetail: Option[GroupDetail] = None,
                         liabilityDetails: LiabilityDetails = LiabilityDetails(),
                         primaryContactDetails: PrimaryContactDetails = PrimaryContactDetails(),
                         organisationDetails: OrganisationDetails = OrganisationDetails(),
                         metaData: MetaData = MetaData(),
                         userHeaders: Option[Map[String, String]] = None
                       ) {

  def toRegistration: Registration =
    Registration(
      id = this.id,
      incorpJourneyId = this.incorpJourneyId,
      registrationType = this.registrationType,
      groupDetail = this.groupDetail,
      liabilityDetails = this.liabilityDetails,
      primaryContactDetails = this.primaryContactDetails,
      organisationDetails = this.organisationDetails,
      metaData = this.metaData
    )

  def checkAndSubmitStatus: TaskStatus =
    if (isRegistrationComplete)
      TaskStatus.Completed
    else if (metaData.registrationReviewed)
      TaskStatus.InProgress
    else if (isCheckAndSubmitReady)
      TaskStatus.NotStarted
    else
      TaskStatus.CannotStartYet

  def numberOfCompletedSections: Int =
    Array(
      isLiabilityDetailsComplete,
      isCompanyDetailsComplete,
      !isPartnershipWithPartnerCollection && isPrimaryContactDetailsComplete,
      isGroup && isOtherOrganisationsInGroupComplete,
      isPartnershipWithPartnerCollection && isNominatedPartnerDetailsComplete && isOtherPartnersDetailsComplete,
      isRegistrationComplete
    ).count(isComplete => isComplete)

  def isGroup: Boolean = registrationType.contains(RegType.GROUP)

  def isPartnership: Boolean = organisationDetails.organisationType.contains(OrgType.PARTNERSHIP)

  def isPartnershipWithPartnerCollection: Boolean =
    organisationDetails.organisationType.contains(OrgType.PARTNERSHIP) &&
      (
        organisationDetails.partnershipDetails.exists(_.partnershipType.contains(PartnerTypeEnum.GENERAL_PARTNERSHIP)) ||
          organisationDetails.partnershipDetails.exists(_.partnershipType.contains(PartnerTypeEnum.SCOTTISH_PARTNERSHIP)) ||
          organisationDetails.partnershipDetails.exists(_.partnershipType.contains(PartnerTypeEnum.LIMITED_PARTNERSHIP)) ||
          organisationDetails.partnershipDetails.exists(_.partnershipType.contains(PartnerTypeEnum.SCOTTISH_LIMITED_PARTNERSHIP))
        )

  def numberOfSections: Int = if (isGroup) 5 else 4

  def isRegistrationComplete: Boolean =
    isCheckAndSubmitReady && metaData.registrationCompleted

  def isCheckAndSubmitReady: Boolean =
    isCompanyDetailsComplete && isLiabilityDetailsComplete && isPrimaryContactDetailsComplete &&
      isOtherOrganisationsInGroupComplete && isPartnerCollectionComplete

  def isCompanyDetailsComplete: Boolean = companyDetailsStatus == TaskStatus.Completed

  def companyDetailsStatus: TaskStatus =
    if (!isLiabilityDetailsComplete)
      TaskStatus.CannotStartYet
    else organisationDetails.status


  def hasCompletedNewLiability: Boolean = {
    hasStartedNewLiabilty && hasFinishedNewLiability
  }

  def hasFinishedNewLiability = liabilityDetails.newLiabilityFinished.isDefined

  def hasStartedNewLiabilty = liabilityDetails.newLiabilityStarted.isDefined

  def newLiabilityInProgress: Boolean = liabilityDetails.newLiabilityStarted.isDefined || liabilityDetails.newLiabilityFinished.isDefined

  def isLiabilityDetailsComplete: Boolean = liabilityDetailsStatus == TaskStatus.Completed

  def libStatusCheck = {
    if (liabilityDetails.isCompleted && registrationType.contains(GROUP))
      if (groupDetail.flatMap(_.membersUnderGroupControl).contains(true))
        TaskStatus.Completed
      else TaskStatus.InProgress
    else if (liabilityDetails.status == TaskStatus.Completed)
      if (registrationType.nonEmpty) TaskStatus.Completed else TaskStatus.InProgress
    else
      liabilityDetails.status
  }

  def liabilityDetailsStatus: TaskStatus = {
    if (!newLiabilityInProgress) TaskStatus.NotStarted
    else if (newLiabilityInProgress) TaskStatus.InProgress else {
      libStatusCheck
    }
  }

  def isPrimaryContactDetailsComplete: Boolean = primaryContactDetailsStatus == TaskStatus.Completed

  def isNominatedPartnerDetailsComplete: Boolean =
    nominatedPartnerDetailsStatus == TaskStatus.Completed

  def primaryContactDetailsStatus: TaskStatus =
    if (companyDetailsStatus != TaskStatus.Completed)
      TaskStatus.CannotStartYet
    else if (isPartnershipWithPartnerCollection) TaskStatus.Completed
    else this.primaryContactDetails.status(metaData.emailVerified)

  def nominatedPartnerDetailsStatus: TaskStatus =
    this.organisationDetails.nominatedPartner match {
      case Some(_) => TaskStatus.Completed
      case _ =>
        if (companyDetailsStatus == TaskStatus.Completed)
          TaskStatus.NotStarted
        else
          TaskStatus.CannotStartYet
    }

  private def isOtherPartnersDetailsComplete: Boolean =
    otherPartnersDetailsStatus == TaskStatus.Completed

  def otherPartnersDetailsStatus: TaskStatus =
    this.organisationDetails.partnershipDetails.map(_.partners.size) match {
      case Some(numPartners) =>
        numPartners match {
          case 0 => TaskStatus.CannotStartYet
          case 1 => TaskStatus.NotStarted
          case _ => TaskStatus.Completed
        }
      case _ => TaskStatus.CannotStartYet
    }

  private def isOtherOrganisationsInGroupComplete: Boolean =
    !isGroup || otherOrganisationsInGroupStatus == TaskStatus.Completed

  private def isPartnerCollectionComplete: Boolean =
    !isPartnershipWithPartnerCollection || otherPartnersDetailsStatus == TaskStatus.Completed

  def otherOrganisationsInGroupStatus: TaskStatus =
    if (!isGroup || primaryContactDetailsStatus != TaskStatus.Completed)
      TaskStatus.CannotStartYet
    else
      groupDetail.map(_.status).getOrElse(TaskStatus.NotStarted)

  def asCompleted(): Registration =
    this.copy(metaData = this.metaData.copy(registrationCompleted = true))

  val isStarted: Boolean = liabilityDetails.status != TaskStatus.NotStarted

  val isFirstGroupMember: Boolean = groupDetail.exists(_.members.isEmpty)

  def populateBusinessRegisteredAddress(addressConversionUtils: AddressConversionUtils): Registration =
    this.copy(organisationDetails =
      this.organisationDetails.withBusinessRegisteredAddress(addressConversionUtils)
    )

  val lastMember: Option[GroupMember] = groupDetail.flatMap(_.members.lastOption)

  def findMember(memberId: String): Option[GroupMember] =
    groupDetail.flatMap(_.findGroupMember(Some(memberId), None))

  def inflightPartner: Option[Partner] =
    organisationDetails.partnershipDetails.flatMap(_.inflightPartner)

  def withInflightPartner(updatedPartner: Option[Partner]): Registration =
    organisationDetails.partnershipDetails.map { partnershipDetails =>
      val updatedPartnershipDetails =
        partnershipDetails.copy(inflightPartner = updatedPartner)
      this.copy(organisationDetails =
        organisationDetails.copy(partnershipDetails = Some(updatedPartnershipDetails))
      )
    }.getOrElse(this)

  def addOtherPartner(otherPartner: Partner): Registration =
    organisationDetails.partnershipDetails.map { partnershipDetails =>
      val updatedOtherPartners =
        partnershipDetails.otherPartners :+ otherPartner
      val updatedPartnershipDetails =
        partnershipDetails.copy(partners = updatedOtherPartners)
      this.copy(organisationDetails =
        organisationDetails.copy(partnershipDetails = Some(updatedPartnershipDetails))
      )
    }.getOrElse(this)

  def withPromotedInflightPartner(): Registration =
    this.copy(organisationDetails =
      organisationDetails.copy(partnershipDetails =
        this.organisationDetails.partnershipDetails.map(_.withPromotedInflightPartner())
      )
    )

  def withUpdatedPartner(partnerId: String, partnerUpdate: Partner => Partner): Registration =
    this.copy(organisationDetails =
      this.organisationDetails.copy(partnershipDetails =
        this.organisationDetails.partnershipDetails.map(_.copy(partners = this.organisationDetails.partnershipDetails.map(_.partners.map {
          partner =>
            if (partner.id == partnerId)
              partnerUpdate(partner)
            else
              partner
        }).getOrElse(Seq())))
      )
    )

  def withUpdatedMemberAddress(memberId: String, address: Address): Registration =
    this.copy(groupDetail = this.groupDetail flatMap {
      group =>
        Some(group.copy(members = group.members map { mem =>
          if (mem.id == memberId)
            mem.copy(addressDetails =
              address
            )
          else
            mem
        }))
    })

  val nominatedPartner: Option[Partner] =
    organisationDetails.partnershipDetails.flatMap(_.nominatedPartner)

  val otherPartners: Seq[Partner] =
    organisationDetails.partnershipDetails.map(_.otherPartners).getOrElse(Seq.empty)

  val newPartner: Option[Partner] = organisationDetails.partnershipDetails.flatMap(_.newPartner)

  def findPartner(partnerId: String): Option[Partner] =
    organisationDetails.partnershipDetails.flatMap(_.findPartner(partnerId))

  def isNominatedPartner(partnerId: Option[String]): Boolean =
    organisationDetails.partnershipDetails.exists(_.isNominatedPartner(partnerId))

  def isNominatedPartnerOrFirstInflightPartner(partner: Partner) = {
    val isTheNominatedPartner = nominatedPartner.exists(_.id == partner.id)
    val isFirstInflightPartner = organisationDetails.partnershipDetails.forall(_.partners.isEmpty)
    isTheNominatedPartner || isFirstInflightPartner
  }

}

object Registration {
  implicit val format: OFormat[Registration] = Json.format[Registration]

}
