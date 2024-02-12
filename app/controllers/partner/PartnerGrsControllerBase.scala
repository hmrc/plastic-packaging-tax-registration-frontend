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

package controllers.partner


import connectors._
import connectors.grs._
import controllers.{routes => commonRoutes}
import forms.organisation.PartnerTypeEnum
import forms.organisation.PartnerTypeEnum._
import models.genericregistration._
import models.registration.{Registration, RegistrationUpdater}
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

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnerGrsControllerBase(
  val journeyAction: ActionBuilder[JourneyRequest, AnyContent],
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  partnershipGrsConnector: PartnershipGrsConnector,
  registeredSocietyGrsConnector: RegisteredSocietyGrsConnector,
  subscriptionsConnector: SubscriptionsConnector,
  val registrationUpdater: RegistrationUpdater,
  mcc: MessagesControllerComponents
)(implicit executionContext: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with Logging {

  protected def grsCallback(journeyId: String, partnerId: Option[String], getRedirect: Call ): Action[AnyContent] =
    journeyAction.async {
      implicit request: JourneyRequest[AnyContent] =>
        saveRegistrationDetails(journeyId, partnerId).flatMap {

          case Right(registration) =>
            registrationStatus(registration, partnerId, getRedirect).map { status =>
              logger.info(
                s"PPT GRS callback for journeyId [$journeyId] " +
                  s"and partner type [${registration.organisationDetails.inflightPartner.getOrElse("")}] " +
                  s"and details [${registration.organisationDetails.partnerGrsRegistration(partnerId)}]"
              )
              status
            }

          case Left(error) => throw error
        }
    }

  private def registrationStatus(registration: Registration, partnerId: Option[String], getRedirect: Call)
    (implicit hc: HeaderCarrier): Future[Result] = {

    registration.organisationDetails.partnerBusinessPartnerId(partnerId) match {

      // We got a business-partner-id, so we can continue
      case Some(businessPartnerId) => checkSubscriptionStatus(businessPartnerId).map {
          case SUBSCRIBED => Redirect(commonRoutes.NotableErrorController.duplicateRegistration())
          case _ => Redirect(getRedirect)
        }

      // No business-partner-id, so what went wrong?
      case None => {
        val maybeRegistrationDetails: Option[RegistrationDetails] = registration.organisationDetails.partnerGrsRegistration(partnerId)
        val result: Result = maybeRegistrationDetails match {
          case Some(RegistrationDetails(false, _, _, None)) => matchingFailed(registration, partnerId)
          case Some(RegistrationDetails(true, _, "REGISTRATION_FAILED", None)) => registrationFailed
          case _ => throw new Exception(s"Unexpected response from GRS during journey-id")
        }
        Future.successful(result)
      }

    }
  }

  private def matchingFailed(registration: Registration, partnerId: Option[String]): Result = {
    // Note this could also handle business-verification fails
    val maybePartnerTypeEnum = registration.organisationDetails.partnerType(partnerId)
    val partnerTypeEnum = maybePartnerTypeEnum.getOrElse(throw new IllegalStateException("Missing partner whilst handling partner callback from GRS"))
    partnerTypeEnum match {
      case PartnerTypeEnum.SOLE_TRADER
           | PartnerTypeEnum.SCOTTISH_PARTNERSHIP
           | PartnerTypeEnum.GENERAL_PARTNERSHIP
           | PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
           | PartnerTypeEnum.SCOTTISH_LIMITED_PARTNERSHIP => Redirect(commonRoutes.NotableErrorController.soleTraderVerificationFailure())
      case _ => Redirect(commonRoutes.NotableErrorController.businessVerificationFailure())
    }
  }

   private def registrationFailed = {
    Redirect(commonRoutes.NotableErrorController.registrationFailed(DateTime.now().toString))
  }

  private def checkSubscriptionStatus(
    businessPartnerId: String
  )(implicit hc: HeaderCarrier): Future[SubscriptionStatus.Status] =
    subscriptionsConnector.getSubscriptionStatus(businessPartnerId).map(_.status)

  private def saveRegistrationDetails(journeyId: String, partnerId: Option[String])(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] = {
    val partnerType: Option[PartnerTypeEnum] = partnerId match {
      case Some(partnerId) => request.registration.findPartner(partnerId).map(_.partnerType)
      case None            => request.registration.inflightPartner.map(_.partnerType)
    }
    val result: Future[Registration] = partnerType match {
      case Some(UK_COMPANY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
        updateUkCompanyDetails(journeyId, partnerId)
      case Some(REGISTERED_SOCIETY) => updateRegisteredSocietyDetails(journeyId, partnerId)
      case Some(SOLE_TRADER)        => updateSoleTraderDetails(journeyId, partnerId)
      case Some(LIMITED_LIABILITY_PARTNERSHIP) | Some(SCOTTISH_LIMITED_PARTNERSHIP) | Some(
            SCOTTISH_PARTNERSHIP
          ) =>
        updatePartnershipDetails(journeyId, partnerId)
      case _ => throw new InternalServerException(s"Invalid organisation type")
    }
    result.map(Right(_)).recover {
      case ex: Exception =>
        Left(
          DownstreamServiceError(s"Failed to retrieve registration, error: ${ex.getMessage}", ex)
        )
    }
  }

  private def updateUkCompanyDetails(journeyId: String, partnerId: Option[String])(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Registration] =
    updateIncorporationDetails(journeyId, partnerId, ukCompanyGrsConnector.getDetails)

  private def updateIncorporationDetails(
    journeyId: String,
    partnerId: Option[String],
    getDetails: String => Future[IncorporationDetails]
  )(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    getDetails(journeyId).flatMap { incorporationDetails =>
      updateRegistration(soleTraderDetails = None,
                         incorporationDetails = Some(incorporationDetails),
                         partnershipDetails = None,
                         partnerId = partnerId
      )
    }

  private def updateRegisteredSocietyDetails(journeyId: String, partnerId: Option[String])(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Registration] =
    updateIncorporationDetails(journeyId, partnerId, registeredSocietyGrsConnector.getDetails)

  private def updateSoleTraderDetails(journeyId: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Registration] =
    soleTraderGrsConnector.getDetails(journeyId).map { soleTraderDetails =>
      updateRegistration(soleTraderDetails = Some(soleTraderDetails),
                         incorporationDetails = None,
                         partnershipDetails = None,
                         partnerId = partnerId
      )
    }.flatten

  private def updatePartnershipDetails(journeyId: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Registration] =
    partnershipGrsConnector.getDetails(journeyId).map { partnershipBusinessDetails =>
      val partnershipDetails = Some(
        PartnerPartnershipDetails(partnershipBusinessDetails = Some(partnershipBusinessDetails))
      )
      updateRegistration(soleTraderDetails = None,
                         incorporationDetails = None,
                         partnershipDetails = partnershipDetails,
                         partnerId = partnerId
      )
    }.flatMap(result => result)

  private def updateRegistration(
    soleTraderDetails: Option[SoleTraderDetails],
    incorporationDetails: Option[IncorporationDetails],
    partnershipDetails: Option[PartnerPartnershipDetails],
    partnerId: Option[String]
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    partnerId match {
      case Some(partnerId) =>
        updateExistingPartner(soleTraderDetails,
                              incorporationDetails,
                              partnershipDetails,
                              partnerId
        )
      case None =>
        updateInflightPartner(soleTraderDetails, incorporationDetails, partnershipDetails)
    }

  private def updateInflightPartner(
    soleTraderDetails: Option[SoleTraderDetails],
    incorporationDetails: Option[IncorporationDetails],
    partnershipDetails: Option[PartnerPartnershipDetails]
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.inflightPartner.map { partner =>
        registration.withInflightPartner(
          Some(
            partnerWithUpdatedGRSDetails(partner,
                                         soleTraderDetails,
                                         incorporationDetails,
                                         partnershipDetails
            )
          )
        )
      }.getOrElse {
        registration
      }
    }

  private def updateExistingPartner(
    soleTraderDetails: Option[SoleTraderDetails],
    incorporationDetails: Option[IncorporationDetails],
    partnershipDetails: Option[PartnerPartnershipDetails],
    partnerId: String
  )(implicit req: JourneyRequest[AnyContent]): Future[Registration] =
    registrationUpdater.updateRegistration { registration =>
      registration.withUpdatedPartner(partnerId,
                                      partner =>
                                        partnerWithUpdatedGRSDetails(partner,
                                                                     soleTraderDetails,
                                                                     incorporationDetails,
                                                                     partnershipDetails
                                        )
      )
    }

  private def partnerWithUpdatedGRSDetails(
    partner: Partner,
    soleTraderDetails: Option[SoleTraderDetails],
    incorporationDetails: Option[IncorporationDetails],
    partnershipDetails: Option[PartnerPartnershipDetails]
  ): Partner = {
    // If we previously prompted the user to supply the name of this partnership,
    // then we persisted it in a location which is about to be overwritten by this fresh GRS callback.
    val partnershipDetailsWithPreservedPartnershipName =
      partnershipDetails.map(
        _.copy(partnershipName =
          if (partner.canEditName)
            partner.partnerPartnershipDetails.flatMap(_.partnershipName)
          else
            None
        )
      )

    partner.copy(soleTraderDetails = soleTraderDetails,
                 incorporationDetails = incorporationDetails,
                 partnerPartnershipDetails = partnershipDetailsWithPreservedPartnershipName
    )
  }

}
