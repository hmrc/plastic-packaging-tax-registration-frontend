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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.RegistrationStatus.{BUSINESS_IDENTIFICATION_FAILED, BUSINESS_VERIFICATION_FAILED, DUPLICATE_SUBSCRIPTION, GRS_FAILED, RegistrationStatus, STATUS_OK, UNSUPPORTED_ORGANISATION}
  GRS_FAILED,
  SOLE_TRADER_VERIFICATION_FAILED,
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{routes => orgRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.SOLE_TRADER
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.PartnershipTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, OrganisationDetails, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.SUBSCRIBED
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object RegistrationStatus extends Enumeration {
  type RegistrationStatus = Value

  val STATUS_OK: Value                       = Value
  val GRS_FAILED: Value                      = Value
  val BUSINESS_VERIFICATION_FAILED: Value    = Value
  val SOLE_TRADER_VERIFICATION_FAILED: Value = Value
  val DUPLICATE_SUBSCRIPTION: Value          = Value
  val UNSUPPORTED_ORGANISATION: Value        = Value
}

@Singleton
class GrsController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  partnershipGrsConnector: PartnershipGrsConnector,
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
                  s"and details [${registration.organisationDetails.grsRegistration.getOrElse("None")}]"
              )
              status match {
                case STATUS_OK =>
                  Redirect(orgRoutes.ConfirmBusinessAddressController.displayPage())
                case GRS_FAILED =>
                  Redirect(commonRoutes.NotableErrorController.grsFailure())
                case BUSINESS_VERIFICATION_FAILED =>
                  Redirect(commonRoutes.NotableErrorController.businessVerificationFailure())
                case SOLE_TRADER_VERIFICATION_FAILED =>
                  Redirect(commonRoutes.NotableErrorController.soleTraderVerificationFailure())
                case DUPLICATE_SUBSCRIPTION =>
                  if (registration.isGroup)
                    Redirect(
                      groupRoutes.NotableErrorController.nominatedOrganisationAlreadyRegistered()
                    )
                  else
                    Redirect(commonRoutes.NotableErrorController.duplicateRegistration())
                case UNSUPPORTED_ORGANISATION =>
                  Redirect(orgRoutes.OrganisationTypeNotSupportedController.onPageLoad())
              }
            }
          case Left(error) => throw error
        }
    }

  private def registrationStatus(
    registration: Registration
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[RegistrationStatus] =
    if (registration.organisationDetails.businessVerificationFailed)
      registration.organisationDetails.organisationType match {
        case Some(SOLE_TRADER) =>
          Future.successful(SOLE_TRADER_VERIFICATION_FAILED)
        case _ =>
          Future.successful(BUSINESS_VERIFICATION_FAILED)
      }
    else
      registration.organisationDetails.businessPartnerId match {
        case Some(businessPartnerId) =>
          checkSubscriptionStatus(businessPartnerId, registration).map {
            case SUBSCRIBED => DUPLICATE_SUBSCRIPTION
            case _          => STATUS_OK
          }
        case None =>
          registration.organisationDetails.grsRegistration match {
            case Some(
                  RegistrationDetails(false, Some("UNCHALLENGED"), "REGISTRATION_NOT_CALLED", _)
                ) =>
              Future.successful(UNSUPPORTED_ORGANISATION)
            case _ => Future.successful(GRS_FAILED)
          }
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
    request.registration.organisationDetails.organisationType match {
      case Some(OrgType.UK_COMPANY) | Some(OrgType.OVERSEAS_COMPANY_UK_BRANCH) =>
        updateUkCompanyDetails(journeyId)
      case Some(OrgType.REGISTERED_SOCIETY) => updateRegisteredSocietyDetails(journeyId)
      case Some(OrgType.SOLE_TRADER)        => updateSoleTraderDetails(journeyId)
      case Some(OrgType.PARTNERSHIP)        => updatePartnershipDetails(journeyId)
      case _                                => throw new InternalServerException(s"Invalid organisation type")
    }
  }.flatMap(
    updatedRegistration => update(_ => updatedRegistration.populateBusinessRegisteredAddress())
  )

  private def updateUkCompanyDetails(
    journeyId: String
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Registration] =
    updateIncorporationDetails(journeyId, ukCompanyGrsConnector.getDetails)

  private def updateRegisteredSocietyDetails(
    journeyId: String
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Registration] =
    updateIncorporationDetails(journeyId, registeredSocietyGrsConnector.getDetails)

  private def updateIncorporationDetails(
    journeyId: String,
    getDetails: String => Future[IncorporationDetails]
  )(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    getDetails(journeyId).map { incorporationDetails =>
      request.registration.copy(incorpJourneyId = Some(journeyId),
                                organisationDetails = request.registration.organisationDetails.copy(
                                  incorporationDetails =
                                    Some(incorporationDetails),
                                  soleTraderDetails = None,
                                  partnershipDetails = None
                                )
      )
    }

  private def updateSoleTraderDetails(
    journeyId: String
  )(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    soleTraderGrsConnector.getDetails(journeyId).map { soleTraderDetails =>
      request.registration.copy(incorpJourneyId = Some(journeyId),
                                organisationDetails = request.registration.organisationDetails.copy(
                                  incorporationDetails = None,
                                  partnershipDetails = None,
                                  soleTraderDetails = Some(soleTraderDetails)
                                )
      )
    }

  private def updatePartnershipDetails(
    journeyId: String
  )(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    request.registration.organisationDetails.partnershipDetails match {
      case Some(partnershipDetails) =>
        partnershipGrsConnector.getDetails(journeyId).map { partnershipBusinessDetails =>
          request.registration.copy(incorpJourneyId = Some(journeyId),
                                    organisationDetails = updatePartnershipDetails(
                                      organisationDetails =
                                        request.registration.organisationDetails,
                                      partnershipBusinessDetails = Some(partnershipBusinessDetails),
                                      partnershipTypeEnum = partnershipDetails.partnershipType
                                    )
          )
        }
      case _ => throw new IllegalStateException("Missing partnership details")
    }

  private def updatePartnershipDetails(
    organisationDetails: OrganisationDetails,
    partnershipBusinessDetails: Option[PartnershipBusinessDetails],
    partnershipTypeEnum: PartnershipTypeEnum
  ): OrganisationDetails = {
    val updatedPartnershipDetails: PartnershipDetails = organisationDetails.partnershipDetails.fold(
      PartnershipDetails(partnershipType = partnershipTypeEnum,
                         partnershipName = None,
                         partnershipBusinessDetails = partnershipBusinessDetails
      )
    )(
      partnershipDetails =>
        partnershipDetails.copy(partnershipBusinessDetails = partnershipBusinessDetails)
    )
    organisationDetails.copy(partnershipDetails = Some(updatedPartnershipDetails))
  }

}
