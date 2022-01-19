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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.RegistrationStatus.{
  DUPLICATE_SUBSCRIPTION,
  RegistrationStatus,
  STATUS_OK,
  UNSUPPORTED_ORGANISATION
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{routes => orgRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum._
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  OrganisationDetails,
  Registration
}
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

  def grsCallback(journeyId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        saveRegistrationDetails(journeyId).flatMap {
          case Right(registration) =>
            registrationStatus(registration).map { status =>
              logger.info(
                s"PPT GRS callback for journeyId [$journeyId] " +
                  s"and organisation type [${registration.organisationDetails.nominatedPartnerType.getOrElse("")}] " +
                  s"had registration status [$status] " +
                  s"and details [${registration.organisationDetails.nominatedPartnerRegistrationStatus.getOrElse("None")}]"
              )
              status match {
                case STATUS_OK =>
                  Redirect(commonRoutes.TaskListController.displayPage())
                case DUPLICATE_SUBSCRIPTION =>
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
    registration.organisationDetails.nominatedPartnerBusinessPartnerId match {
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

  private def saveRegistrationDetails(journeyId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] = {
    request.registration.organisationDetails.nominatedPartnerType match {
      case Some(UK_COMPANY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
        updateUkCompanyDetails(journeyId)
      case Some(SOLE_TRADER) => updateSoleTraderDetails(journeyId)
      case Some(LIMITED_LIABILITY_PARTNERSHIP) | Some(SCOTTISH_LIMITED_PARTNERSHIP) |
          Some(SCOTTISH_PARTNERSHIP) =>
        updatePartnershipDetails(journeyId)
      case _ => throw new InternalServerException(s"Invalid organisation type")
    }
  }.flatMap(updatedRegistration => update(_ => updatedRegistration))

  private def updateUkCompanyDetails(
    journeyId: String
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Registration] =
    updateIncorporationDetails(journeyId, ukCompanyGrsConnector.getDetails)

  private def updateIncorporationDetails(
    journeyId: String,
    getDetails: String => Future[IncorporationDetails]
  )(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    getDetails(journeyId).map { incorporationDetails =>
      request.registration.copy(organisationDetails =
        updateOrganisationDetails(request.registration.organisationDetails,
                                  soleTraderDetails = None,
                                  incorporationDetails = Some(incorporationDetails),
                                  partnershipDetails = None
        )
      )
    }

  private def updateSoleTraderDetails(
    journeyId: String
  )(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    soleTraderGrsConnector.getDetails(journeyId).map { soleTraderDetails =>
      request.registration.copy(organisationDetails =
        updateOrganisationDetails(request.registration.organisationDetails,
                                  soleTraderDetails = Some(soleTraderDetails),
                                  incorporationDetails = None,
                                  partnershipDetails = None
        )
      )
    }

  private def updatePartnershipDetails(
    journeyId: String
  )(implicit request: JourneyRequest[AnyContent]): Future[Registration] =
    partnershipGrsConnector.getDetails(journeyId).map { partnershipBusinessDetails =>
      val partnershipDetails = Some(
        PartnerPartnershipDetails(
          partnershipType = request.registration.organisationDetails.nominatedPartnerType.get,
          partnershipName = None,
          partnershipBusinessDetails = Some(partnershipBusinessDetails)
        )
      )
      request.registration.copy(organisationDetails =
        updateOrganisationDetails(organisationDetails = request.registration.organisationDetails,
                                  soleTraderDetails = None,
                                  incorporationDetails = None,
                                  partnershipDetails = partnershipDetails
        )
      )
    }

  private def updateOrganisationDetails(
    organisationDetails: OrganisationDetails,
    soleTraderDetails: Option[SoleTraderDetails],
    incorporationDetails: Option[IncorporationDetails],
    partnershipDetails: Option[PartnerPartnershipDetails]
  ) =
    organisationDetails.copy(partnershipDetails =
      Some(
        organisationDetails.partnershipDetails.map(
          details =>
            details.copy(nominatedPartner =
              updatedNominatedPartner(details = details,
                                      soleTraderDetails = soleTraderDetails,
                                      incorporationDetails = incorporationDetails,
                                      partnershipDetails = partnershipDetails
              )
            )
        ).getOrElse(throw new IllegalStateException("No partnership details found"))
      )
    )

  private def updatedNominatedPartner(
    details: PartnershipDetails,
    soleTraderDetails: Option[SoleTraderDetails],
    incorporationDetails: Option[IncorporationDetails],
    partnershipDetails: Option[PartnerPartnershipDetails]
  ): Option[Partner] =
    details.nominatedPartner match {
      case Some(partner) =>
        Some(
          partner.copy(soleTraderDetails = soleTraderDetails,
                       incorporationDetails = incorporationDetails,
                       partnerPartnershipDetails = partnershipDetails
          )
        )
      case _ => throw new IllegalStateException("No nominated partner found")
    }

}
