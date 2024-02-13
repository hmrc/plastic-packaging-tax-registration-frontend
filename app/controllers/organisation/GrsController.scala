/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.organisation

import connectors._
import connectors.grs._
import controllers.actions.JourneyAction
import controllers.group.{routes => groupRoutes}
import controllers.organisation.{routes => orgRoutes}
import controllers.{routes => commonRoutes}
import forms.organisation.OrgType
import forms.organisation.OrgType.SOLE_TRADER
import forms.organisation.PartnerTypeEnum.PartnerTypeEnum
import models.genericregistration._
import models.registration.{Cacheable, OrganisationDetails, Registration}
import models.request.JourneyRequest
import models.subscriptions.SubscriptionStatus
import models.subscriptions.SubscriptionStatus.SUBSCRIBED
import org.joda.time.DateTime
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AddressConversionUtils

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
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  partnershipGrsConnector: PartnershipGrsConnector,
  subscriptionsConnector: SubscriptionsConnector,
  addressConversionUtils: AddressConversionUtils,
  mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport with Logging {

  def grsCallback(journeyId: String): Action[AnyContent] =
    journeyAction.register.async {
      implicit request =>
        saveRegistrationDetails(journeyId).flatMap {
          case Right(registration) =>
            logger.info(
              s"PPT GRS callback for journeyId [$journeyId] " +
                s"and organisation type [${registration.organisationDetails.organisationType}] " +
                s"and details [${registration.organisationDetails.grsRegistration}]"
            )

            if (registration.organisationDetails.businessVerificationFailed)
              Future.successful(businessVerificationFailed(registration))
            else
              registration.organisationDetails.businessPartnerId match {
                case Some(businessPartnerId) =>
                  checkSubscriptionStatus(businessPartnerId, registration).map {
                    case SUBSCRIBED => duplicateSubscription(registration)
                    case _          => Redirect(orgRoutes.ConfirmBusinessAddressController.displayPage())
                  }
                case None =>
                  Future.successful(registration.organisationDetails.grsRegistration match {
                    case Some(RegistrationDetails(false, _, _, None))                    => matchingFailed(registration)
                    case Some(RegistrationDetails(true, _, "REGISTRATION_FAILED", None)) => registrationFailed
                    case _                                                               => throw new Exception(s"Unexpected response from GRS during journey-id $journeyId")
                  })
              }
          case Left(error) => throw error
        }

    }

  //this is very similar to below, do we want to combine them?
  private def businessVerificationFailed(registration: Registration): Result =
    registration.organisationDetails.organisationType match {
      case Some(SOLE_TRADER) | Some(OrgType.PARTNERSHIP) =>
        Redirect(commonRoutes.NotableErrorController.soleTraderVerificationFailure())
      case _ =>
        Redirect(commonRoutes.NotableErrorController.businessVerificationFailure())
    }

  private def matchingFailed(registration: Registration): Result = {
    // Note this could also handle business-verification fails
    val orgTypeEnum = registration.organisationDetails.organisationType.getOrElse(throw new IllegalStateException("Missing OrgType whilst handling organisation callback from GRS"))
    orgTypeEnum match {
      case OrgType.SOLE_TRADER | OrgType.PARTNERSHIP => Redirect(commonRoutes.NotableErrorController.soleTraderVerificationFailure())
      case _                                         => Redirect(commonRoutes.NotableErrorController.businessVerificationFailure())
    }
  }

  private def registrationFailed =
    Redirect(commonRoutes.NotableErrorController.registrationFailed(DateTime.now().toString))

  private def duplicateSubscription(registration: Registration) =
    if (registration.isGroup) Redirect(groupRoutes.NotableErrorController.nominatedOrganisationAlreadyRegistered())
    else Redirect(commonRoutes.NotableErrorController.duplicateRegistration())

  private def checkSubscriptionStatus(businessPartnerId: String, registration: Registration)(implicit
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

  private def saveRegistrationDetails(journeyId: String)(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] = {
    request.registration.organisationDetails.organisationType match {
      case Some(OrgType.UK_COMPANY) | Some(OrgType.OVERSEAS_COMPANY_UK_BRANCH) =>
        updateUkCompanyDetails(journeyId)
      case Some(OrgType.REGISTERED_SOCIETY) => updateRegisteredSocietyDetails(journeyId)
      case Some(OrgType.SOLE_TRADER)        => updateSoleTraderDetails(journeyId)
      case Some(OrgType.PARTNERSHIP)        => updatePartnershipDetails(journeyId)
      case _                                => throw new InternalServerException(s"Invalid organisation type")
    }
  }.flatMap(updatedRegistration => update(_ => updatedRegistration.populateBusinessRegisteredAddress(addressConversionUtils)))

  private def updateUkCompanyDetails(journeyId: String)(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Registration] =
    updateIncorporationDetails(journeyId, ukCompanyGrsConnector.getDetails)

  private def updateRegisteredSocietyDetails(journeyId: String)(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Registration] =
    updateIncorporationDetails(journeyId, registeredSocietyGrsConnector.getDetails)

  private def updateIncorporationDetails(journeyId: String, getDetails: String => Future[IncorporationDetails])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Registration] =
    getDetails(journeyId).map { incorporationDetails =>
      request.registration.copy(
        incorpJourneyId = Some(journeyId),
        organisationDetails = request.registration.organisationDetails.copy(
          incorporationDetails =
            Some(incorporationDetails),
          soleTraderDetails = None,
          partnershipDetails = None
        )
      )
    }

  private def updateSoleTraderDetails(journeyId: String)(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    soleTraderGrsConnector.getDetails(journeyId).map { soleTraderDetails =>
      request.registration.copy(
        incorpJourneyId = Some(journeyId),
        organisationDetails = request.registration.organisationDetails.copy(incorporationDetails = None, partnershipDetails = None, soleTraderDetails = Some(soleTraderDetails))
      )
    }

  private def updatePartnershipDetails(journeyId: String)(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    request.registration.organisationDetails.partnershipDetails match {
      case Some(partnershipDetails) =>
        partnershipGrsConnector.getDetails(journeyId).map { partnershipBusinessDetails =>
          request.registration.copy(
            incorpJourneyId = Some(journeyId),
            organisationDetails = updatePartnershipDetails(
              organisationDetails =
                request.registration.organisationDetails,
              partnershipBusinessDetails = Some(partnershipBusinessDetails),
              partnerTypeEnum = partnershipDetails.partnershipType
            )
          )
        }
      case _ => throw new IllegalStateException("Missing partnership details")
    }

  private def updatePartnershipDetails(
    organisationDetails: OrganisationDetails,
    partnershipBusinessDetails: Option[PartnershipBusinessDetails],
    partnerTypeEnum: PartnerTypeEnum
  ): OrganisationDetails = {
    val updatedPartnershipDetails: PartnershipDetails = organisationDetails.partnershipDetails.fold(
      PartnershipDetails(partnershipType = partnerTypeEnum, partnershipName = None, partnershipBusinessDetails = partnershipBusinessDetails)
    )(partnershipDetails => partnershipDetails.copy(partnershipBusinessDetails = partnershipBusinessDetails))
    organisationDetails.copy(partnershipDetails = Some(updatedPartnershipDetails))
  }

}
