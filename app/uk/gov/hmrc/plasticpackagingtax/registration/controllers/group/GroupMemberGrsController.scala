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

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.RegistrationStatus.{
  BUSINESS_IDENTIFICATION_FAILED,
  BUSINESS_VERIFICATION_FAILED,
  DUPLICATE_SUBSCRIPTION,
  RegistrationStatus,
  STATUS_OK
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
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

object RegistrationStatus extends Enumeration {
  type RegistrationStatus = Value

  val STATUS_OK: Value                      = Value
  val BUSINESS_IDENTIFICATION_FAILED: Value = Value
  val BUSINESS_VERIFICATION_FAILED: Value   = Value
  val DUPLICATE_SUBSCRIPTION: Value         = Value
}

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

  private val logger = Logger(this.getClass)

  def grsCallback(journeyId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        saveRegistrationDetails(journeyId).flatMap {
          case Right(registration) =>
            registrationStatus(registration).map { status =>
              logger.info(
                s"PPT GRS callback for journeyId [$journeyId] " +
                  s"and organisation type [${registration.organisationDetails.organisationType.getOrElse("")}] " +
                  s"had registration status [$status] " +
                  s"and details [${registration.organisationDetails.registrationDetails.getOrElse("None")}]"
              )
              Redirect(routes.OrganisationDetailsTypeController.displayPage())

            }
          case Left(error) => throw error
        }
    }

  private def registrationStatus(
    registration: Registration
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[RegistrationStatus] =
    if (registration.organisationDetails.businessVerificationFailed)
      Future.successful(BUSINESS_VERIFICATION_FAILED)
    else
      registration.organisationDetails.businessPartnerId match {
        case Some(businessPartnerId) => Future.successful(STATUS_OK)
        case None                    => Future.successful(BUSINESS_IDENTIFICATION_FAILED)
      }

  private def checkSubscriptionStatus(businessPartnerId: String, registration: Registration)(
    implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[SubscriptionStatus.Status] =
    subscriptionsConnector.getSubscriptionStatus(businessPartnerId).flatMap {
      response =>
        update(
          model =>
            model.copy(
              incorpJourneyId = registration.incorpJourneyId,
              organisationDetails =
                registration.organisationDetails.copy(subscriptionStatus = Some(response.status))
            )
        ).map { _ =>
          response.status
        }
    }

  private def saveRegistrationDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] = {
    val orgType: Option[OrgType] =
      request.registration.groupDetail.flatMap(_.currentMemberOrganisationType)
    orgType match {
      case Some(OrgType.UK_COMPANY) => updateUkCompanyDetails(journeyId, OrgType.UK_COMPANY)
      case _                        => throw new InternalServerException(s"Invalid organisation type")
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
      result <- update { registration =>
        val updatedGroupDetails: GroupDetail = registration.groupDetail match {
          case Some(value) =>
            val members: Seq[GroupMember] = value.members :+ addGroupMember(details, orgType)
            value.copy(members = members)
          case None => throw new InternalServerException(s"No group detail")
        }
        registration.copy(groupDetail = Some(updatedGroupDetails))
      }
    } yield result

  private def addGroupMember(details: IncorporationDetails, orgType: OrgType)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): GroupMember =
    GroupMember(customerIdentification1 = details.companyNumber,
                customerIdentification2 = Some(details.ctutr),
                organisationDetails =
                  Some(OrganisationDetails(orgType.toString, details.companyName)),
                addressDetails = details.companyAddress.toGroupAddressDetails
    )

}
