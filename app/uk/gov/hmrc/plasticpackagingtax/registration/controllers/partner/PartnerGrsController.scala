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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.RegistrationStatus.{
  DUPLICATE_SUBSCRIPTION,
  RegistrationStatus,
  STATUS_OK,
  UNSUPPORTED_ORGANISATION
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{routes => orgRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum._
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.SUBSCRIBED
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnerGrsController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  ukCompanyGrsConnector: UkCompanyGrsConnector,
  soleTraderGrsConnector: SoleTraderGrsConnector,
  partnershipGrsConnector: PartnershipGrsConnector,
  subscriptionsConnector: SubscriptionsConnector,
  mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  private val logger = Logger(this.getClass)

  def grsCallbackNewPartner(journeyId: String): Action[AnyContent] = grsCallback(journeyId, None)

  def grsCallbackExistingPartner(journeyId: String, partnerId: String): Action[AnyContent] =
    grsCallback(journeyId, Some(partnerId))

  private def grsCallback(journeyId: String, partnerId: Option[String]): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        saveRegistrationDetails(journeyId, partnerId).flatMap {
          case Right(registration) =>
            registrationStatus(registration, partnerId).map { status =>
              logger.info(
                s"PPT GRS callback for journeyId [$journeyId] " +
                  s"and partner type [${registration.organisationDetails.inflightPartner.getOrElse("")}] " +
                  s"had registration status [$status] " +
                  s"and details [${registration.organisationDetails.partnerRegistrationStatus(partnerId).getOrElse("None")}]"
              )
              status match {
                case STATUS_OK =>
                  partnerId match {
                    case Some(partnerId) =>
                      Redirect(
                        partnerRoutes.PartnerContactNameController.displayExistingPartner(partnerId)
                      )
                    case None =>
                      Redirect(partnerRoutes.PartnerContactNameController.displayNewPartner())
                  }
                case DUPLICATE_SUBSCRIPTION =>
                  Redirect(commonRoutes.NotableErrorController.duplicateRegistration())
                case UNSUPPORTED_ORGANISATION =>
                  Redirect(orgRoutes.OrganisationTypeNotSupportedController.onPageLoad())
              }
            }
          case Left(error) => throw error
        }
    }

  private def registrationStatus(registration: Registration, partnerId: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[RegistrationStatus] =
    registration.organisationDetails.partnerBusinessPartnerId(partnerId) match {
      case Some(businessPartnerId) =>
        checkSubscriptionStatus(businessPartnerId).map {
          case SUBSCRIBED => DUPLICATE_SUBSCRIPTION
          case _          => STATUS_OK
        }
      case None =>
        Future.successful(UNSUPPORTED_ORGANISATION)
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
      case Some(partnerId) => request.registration.findPartner(partnerId).flatMap(_.partnerType)
      case None            => request.registration.inflightPartner.flatMap(_.partnerType)
    }
    partnerType match {
      case Some(UK_COMPANY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
        updateUkCompanyDetails(journeyId, partnerId)
      case Some(SOLE_TRADER) => updateSoleTraderDetails(journeyId, partnerId)
      case Some(LIMITED_LIABILITY_PARTNERSHIP) | Some(SCOTTISH_LIMITED_PARTNERSHIP) | Some(
            SCOTTISH_PARTNERSHIP
          ) =>
        updatePartnershipDetails(journeyId, partnerId)
      case _ => throw new InternalServerException(s"Invalid organisation type")
    }
  }

  private def updateUkCompanyDetails(journeyId: String, partnerId: Option[String])(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    updateIncorporationDetails(journeyId, partnerId, ukCompanyGrsConnector.getDetails)

  private def updateIncorporationDetails(
    journeyId: String,
    partnerId: Option[String],
    getDetails: String => Future[IncorporationDetails]
  )(implicit request: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    getDetails(journeyId).map { incorporationDetails =>
      updateRegistration(soleTraderDetails = None,
                         incorporationDetails = Some(incorporationDetails),
                         partnershipDetails = None,
                         partnerId = partnerId
      )
    }.flatMap(result => result)

  private def updateSoleTraderDetails(journeyId: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    soleTraderGrsConnector.getDetails(journeyId).map { soleTraderDetails =>
      updateRegistration(soleTraderDetails = Some(soleTraderDetails),
                         incorporationDetails = None,
                         partnershipDetails = None,
                         partnerId = partnerId
      )
    }.flatMap(result => result)

  private def updatePartnershipDetails(journeyId: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    partnershipGrsConnector.getDetails(journeyId).map { partnershipBusinessDetails =>
      val partnershipDetails = Some(
        PartnerPartnershipDetails(
          partnershipType = request.registration.organisationDetails.partnerType(partnerId).get,
          partnershipBusinessDetails = Some(partnershipBusinessDetails)
        )
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
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
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
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
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
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
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
  ) =
    partner.copy(soleTraderDetails = soleTraderDetails,
                 incorporationDetails = incorporationDetails,
                 partnerPartnershipDetails = partnershipDetails
    )

}
