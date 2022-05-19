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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{PartnershipGrsConnector, UkCompanyGrsConnector}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthActioning
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.RegistrationStatus.{DUPLICATE_SUBSCRIPTION, RegistrationStatus, STATUS_OK}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{CompanyProfile, IncorporationDetails, PartnershipBusinessDetails}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{GroupError, GroupErrorType, GroupMember, OrganisationDetails}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{GroupDetail, Registration, RegistrationUpdater}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{AuthenticatedRequest, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.SUBSCRIBED
import uk.gov.hmrc.plasticpackagingtax.registration.utils.AddressConversionUtils
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

abstract class GroupMemberGrsControllerBase(
  val authenticate: AuthActioning,
  val journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  subscriptionsConnector: SubscriptionsConnector,
  partnershipGrsConnector: PartnershipGrsConnector,
  val registrationUpdater: RegistrationUpdater,
  val addressConversionUtils: AddressConversionUtils,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def findCurrentMember(memberId: Option[String], registration: Registration): String =
    memberId.fold(registration.lastMember)(registration.findMember).getOrElse(
      throw new IllegalStateException("GRS member callback with no group members found")
    ).id

  protected def grsCallback(journeyId: String, memberId: Option[String], getRedirect: String => Call): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        updateRegistrationDetails(journeyId, memberId).flatMap {
          case Right(registration) =>
            registrationStatus(registration, memberId).flatMap {
              case STATUS_OK =>
                save(registration).map { _ =>
                  val member = findCurrentMember(memberId, registration)
                  Redirect(routes.ConfirmBusinessAddressController.displayPage(member, getRedirect(member).url))
                }
              case DUPLICATE_SUBSCRIPTION =>
                val groupError = GroupError(GroupErrorType.MEMBER_IS_ALREADY_REGISTERED, groupMemberName(registration, memberId))
                updateWithGroupError(groupError).map(_ => Redirect(routes.NotableErrorController.groupMemberAlreadyRegistered()))
            }
          case Left(groupError) =>
            updateWithGroupError(groupError).map(_ => Redirect(routes.NotableErrorController.organisationAlreadyInGroup()))
        }
    }

  private def registrationStatus(registration: Registration, memberId: Option[String])(implicit hc: HeaderCarrier): Future[RegistrationStatus] = {
    val organisationDetails: Option[OrganisationDetails] =
      registration.findMember(
        memberId.getOrElse(registration.lastMember.map(_.id).getOrElse(throw new IllegalStateException("Expected group member missing")))
      ).flatMap(_.organisationDetails)

    val status: Future[SubscriptionStatus.Status] =
      organisationDetails.flatMap(_.businessPartnerId) match {
        case Some(businessPartnerId) => checkSubscriptionStatus(businessPartnerId)
        case _                       => Future.successful(SubscriptionStatus.NOT_SUBSCRIBED)
      }
    status.map {
      case SUBSCRIBED => DUPLICATE_SUBSCRIPTION
      case _          => STATUS_OK
    }
  }

  private def checkSubscriptionStatus(businessPartnerId: String)(implicit hc: HeaderCarrier): Future[SubscriptionStatus.Status] =
    subscriptionsConnector.getSubscriptionStatus(businessPartnerId).map(_.status)

  private def updateRegistrationDetails(journeyId: String, memberId: Option[String])(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[GroupError, Registration]] = {
    val orgType: Option[OrgType] =
      request.registration.groupDetail.flatMap(_.currentMemberOrganisationType)
    orgType match {
      case Some(value) if value == OrgType.UK_COMPANY | value == OrgType.OVERSEAS_COMPANY_UK_BRANCH =>
        updateUkCompanyDetails(journeyId, memberId, value)
      case Some(OrgType.PARTNERSHIP) =>
        updatePartnershipDetails(journeyId, memberId, OrgType.PARTNERSHIP)
      case _ => throw new IllegalStateException(s"Invalid organisation type")
    }
  }

  private def updateUkCompanyDetails(journeyId: String, memberId: Option[String], orgType: OrgType)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[GroupError, Registration]] =
    updateGroupMemberDetails(journeyId, memberId, orgType, ukCompanyGrsConnector.getDetails)

  private def updateGroupMemberDetails(
    journeyId: String,
    memberId: Option[String],
    orgType: OrgType,
    getDetails: String => Future[IncorporationDetails]
  )(implicit request: JourneyRequest[AnyContent]): Future[Either[GroupError, Registration]] =
    for {
      details <- getDetails(journeyId)
      groupMember = updateOrCreateMember(details, orgType, memberId)
      result      = updateGroupDetails(groupMember)
    } yield result

  private def updateOrCreateMember(details: IncorporationDetails, orgType: OrgType, memberId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): GroupMember = {

    def updatedGroupMember(groupMember: GroupMember): GroupMember = {
      groupMember.copy(
        customerIdentification1 = details.companyNumber,
        customerIdentification2 = Some(details.ctutr),
        organisationDetails = Some(
          OrganisationDetails(
            organisationType = orgType.toString,
            organisationName = details.companyName,
            businessPartnerId =
              details.registration.flatMap(_.registeredBusinessPartnerId)
          )
        ),
        addressDetails = addressConversionUtils.toPptAddress(details.companyAddress)
          .getOrElse(throw new IllegalStateException("Failed to create PPT address from company address"))
      )
    }

    request.registration.groupDetail.map { groupDetails =>
      groupDetails.findGroupMember(memberId, Some(details.companyNumber)) match {
        case None => createGroupMember(details, orgType)
        case Some(member) => updatedGroupMember(member)
      }
    }.getOrElse(createGroupMember(details, orgType))
  }

  private def createGroupMember(details: IncorporationDetails, orgType: OrgType): GroupMember =
    GroupMember(
      customerIdentification1 = details.companyNumber,
      customerIdentification2 = Some(details.ctutr),
      organisationDetails =
        Some(
          OrganisationDetails(
            orgType.toString,
            details.companyName,
            details.registration.flatMap { reg =>
              reg.registeredBusinessPartnerId
            }
          )
        ),
      addressDetails = addressConversionUtils.toPptAddress(details.companyAddress)
        .getOrElse(throw new IllegalStateException("Failed to create PPT address from company address"))
    )

  private def updatePartnershipDetails(journeyId: String, memberId: Option[String], orgType: OrgType)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[GroupError, Registration]] =
    for {
      details <- partnershipGrsConnector.getDetails(journeyId)
      groupMember = updateOrCreateMember(details, orgType, memberId)
      result      = updateGroupDetails(groupMember)
    } yield result

  private def updateOrCreateMember(details: PartnershipBusinessDetails, orgType: OrgType, memberId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): GroupMember = {

    def updatedGroupMember(groupMember: GroupMember, companyProfile: CompanyProfile): GroupMember = {
      groupMember.copy(
        customerIdentification1 = companyProfile.companyNumber,
        customerIdentification2 = Some(details.sautr),
        organisationDetails = Some(
          OrganisationDetails(
            organisationType = orgType.toString,
            organisationName =
              companyProfile.companyName,
            businessPartnerId =
              details.registration.flatMap(_.registeredBusinessPartnerId)
          )
        ),
        addressDetails = addressConversionUtils.toPptAddress(companyProfile.companyAddress)
          .getOrElse(throw new IllegalStateException("Failed to create PPT address from company address"))
      )
    }

    request.registration.groupDetail.map { groupDetails =>
      details.companyProfile match {
        case Some(companyProfile) =>
          groupDetails.findGroupMember(memberId, Some(companyProfile.companyNumber)) match {
          case None => createGroupMember(details, orgType)
          case Some(member) => updatedGroupMember(member, companyProfile)
        }
        case None => throw new IllegalStateException("No Company Profile")
      }

    }.getOrElse(createGroupMember(details, orgType))
  }

  private def createGroupMember(details: PartnershipBusinessDetails, orgType: OrgType): GroupMember = {
    val companyProfile = details.companyProfile.fold(throw new IllegalStateException("No Company Profile"))(profile => profile)
    GroupMember(
      customerIdentification1 = companyProfile.companyNumber,
      customerIdentification2 = Some(details.sautr),
      organisationDetails =
        Some(
          OrganisationDetails(
            orgType.toString,
            companyProfile.companyName,
            details.registration.flatMap { reg =>
              reg.registeredBusinessPartnerId
            }
          )
        ),
      addressDetails = addressConversionUtils.toPptAddress(companyProfile.companyAddress)
        .getOrElse(throw new IllegalStateException("Failed to create PPT address from company address"))
    )
  }

  private def updateGroupDetails(groupMember: GroupMember)(implicit request: JourneyRequest[_]): Either[GroupError, Registration] = {

    val registration = request.registration

    registration.groupDetail match {
      case Some(groupDetail) =>
          Right(
            registration.copy(groupDetail =
              Some(groupDetail.withUpdatedOrNewMember(groupMember))
            )
          )
      case None => throw new IllegalStateException(s"No group detail")
    }

  }

  def isMemberNominated(member: GroupMember, groupDetail: GroupDetail, registration: Registration): Boolean =
    groupDetail.members.contains(member) ||
      registration.organisationDetails.incorporationDetails.exists(details => details.isGroupMemberSameAsNominated(member.customerIdentification1)) ||
      registration.organisationDetails.partnershipDetails.exists(
        details => details.isGroupMemberSameAsNominatedPartnership(member.customerIdentification1)
      )

  private def save(registration: Registration)(implicit hc: HeaderCarrier, request: JourneyRequest[_]) =
    registrationUpdater.updateRegistration(_ => registration)

  private def updateWithGroupError(groupError: GroupError)(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration(
      registration =>
        registration.copy(groupDetail =
          registration.groupDetail.map(_.copy(groupError = Some(groupError)))
        )
    )

  private def groupMemberName(registration: Registration, memberId: Option[String]): String = {
    val organisationName = memberId match {
      case Some(memberId) => registration.groupDetail.flatMap(_.businessName(memberId))
      case _              => registration.groupDetail.flatMap(_.latestMember.map(_.businessName))
    }
    organisationName.getOrElse("Your organisation")
  }

}
