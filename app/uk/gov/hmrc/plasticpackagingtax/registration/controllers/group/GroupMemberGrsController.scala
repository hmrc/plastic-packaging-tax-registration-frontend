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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.RegistrationStatus.{
  DUPLICATE_SUBSCRIPTION,
  RegistrationStatus,
  STATUS_OK
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  GroupMember,
  OrganisationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  GroupDetail,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.SUBSCRIBED
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GroupMemberGrsController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  subscriptionsConnector: SubscriptionsConnector,
  mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def grsCallback(journeyId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        saveRegistrationDetails(journeyId).flatMap {
          case Right(registration) =>
            registrationStatus(registration).map {
              case STATUS_OK => Redirect(routes.OrganisationListController.displayPage())
              case DUPLICATE_SUBSCRIPTION =>
                Redirect(pptRoutes.NotableErrorController.duplicateRegistration())
            }
          case Left(error) => throw error
        }
    }

  private def registrationStatus(
    registration: Registration
  )(implicit hc: HeaderCarrier): Future[RegistrationStatus] = {
    val organisationDetails: Option[OrganisationDetails] =
      registration.groupDetail.flatMap(
        _.members.lastOption.flatMap(member => member.organisationDetails)
      )
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

  private def checkSubscriptionStatus(
    businessPartnerId: String
  )(implicit hc: HeaderCarrier): Future[SubscriptionStatus.Status] =
    subscriptionsConnector.getSubscriptionStatus(businessPartnerId).map(_.status)

  private def saveRegistrationDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] = {
    val orgType: Option[OrgType] =
      request.registration.groupDetail.flatMap(_.currentMemberOrganisationType)
    orgType match {
      case Some(OrgType.UK_COMPANY) => updateUkCompanyDetails(journeyId, OrgType.UK_COMPANY)
      case _                        => throw new IllegalStateException(s"Invalid organisation type")
    }
  }

  private def updateUkCompanyDetails(journeyId: String, orgType: OrgType)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    updateGroupMemberDetails(journeyId, orgType, ukCompanyGrsConnector.getDetails)

  private def updateGroupMemberDetails(
    journeyId: String,
    orgType: OrgType,
    getDetails: String => Future[IncorporationDetails]
  )(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    for {
      details <- getDetails(journeyId)
      result  <- updateGroupDetails(orgType, details)
    } yield result

  private def addGroupMember(details: IncorporationDetails, orgType: OrgType): GroupMember =
    GroupMember(customerIdentification1 = details.companyNumber,
                customerIdentification2 = Some(details.ctutr),
                organisationDetails =
                  Some(
                    OrganisationDetails(orgType.toString,
                                        details.companyName,
                                        details.registration.registeredBusinessPartnerId
                    )
                  ),
                addressDetails = details.companyAddress.toGroupAddressDetails
    )

  private def updateGroupDetails(orgType: OrgType, details: IncorporationDetails)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[_]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedGroupDetails: GroupDetail = registration.groupDetail match {
        case Some(groupDetail) =>
          val member: GroupMember = addGroupMember(details, orgType)
          if (isMemberPresent(member.customerIdentification1, groupDetail, registration))
            groupDetail
          else {
            val members: Seq[GroupMember] = groupDetail.members :+ member
            groupDetail.copy(members = members, currentMemberOrganisationType = None)
          }
        case None => throw new IllegalStateException(s"No group detail")
      }
      registration.copy(groupDetail = Some(updatedGroupDetails))
    }

  def isMemberPresent(
    customerIdentification1: String,
    groupDetail: GroupDetail,
    registration: Registration
  ): Boolean =
    groupDetail.members.exists(
      groupMember => groupMember.isMemberAlreadyPresent(customerIdentification1)
    ) ||
      registration.organisationDetails.incorporationDetails.exists(
        details => details.isGroupMemberSameAsNominated(customerIdentification1)
      )

}
