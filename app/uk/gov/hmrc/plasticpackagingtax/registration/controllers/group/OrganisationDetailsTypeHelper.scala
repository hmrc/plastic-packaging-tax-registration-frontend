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
import play.api.mvc.Results.{Redirect, SeeOther}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.{AppConfig, Features}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  PartnershipGrsConnector,
  RegisteredSocietyGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{
  routes => organisationRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.PartnershipTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{
  OrgType,
  OrganisationType,
  PartnershipTypeEnum
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorpEntityGrsCreateRequest,
  PartnershipDetails,
  PartnershipGrsCreateRequest,
  SoleTraderGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest

import scala.concurrent.{ExecutionContext, Future}

trait OrganisationDetailsTypeHelper extends Cacheable with I18nSupport {

  def soleTraderGrsConnector: SoleTraderGrsConnector
  def ukCompanyGrsConnector: UkCompanyGrsConnector
  def partnershipGrsConnector: PartnershipGrsConnector
  def registeredSocietyGrsConnector: RegisteredSocietyGrsConnector
  def registrationConnector: RegistrationConnector
  def appConfig: AppConfig

  protected def handleOrganisationType(
    organisationType: OrganisationType,
    businessVerificationCheck: Boolean = true
  )(implicit
    request: JourneyRequest[AnyContent],
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier
  ) =
    (organisationType.answer, request.isFeatureFlagEnabled(Features.isUkCompanyPrivateBeta)) match {
      case (Some(OrgType.UK_COMPANY), _) =>
        getUkCompanyRedirectUrl(businessVerificationCheck)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.OVERSEAS_COMPANY_UK_BRANCH), false) =>
        getUkCompanyRedirectUrl(businessVerificationCheck)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.SOLE_TRADER), false) =>
        getSoleTraderRedirectUrl()
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.REGISTERED_SOCIETY), false) =>
        getRegisteredSocietyRedirectUrl()
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.PARTNERSHIP), false) =>
        if (request.registration.isGroup) {
          updateRegistration(PartnershipTypeEnum.LIMITED_LIABILITY_PARTNERSHIP)
          getRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl,
                         businessVerificationCheck
          ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
        } else
          Future(Redirect(organisationRoutes.PartnershipTypeController.displayPage()))
      case _ =>
        Future(Redirect(organisationRoutes.OrganisationTypeNotSupportedController.onPageLoad()))
    }

  def grsCallbackUrl(): String

  private def getSoleTraderRedirectUrl()(implicit
    request: JourneyRequest[AnyContent],
    headerCarrier: HeaderCarrier
  ): Future[String] =
    soleTraderGrsConnector.createJourney(
      SoleTraderGrsCreateRequest(grsCallbackUrl,
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink
      )
    )

  private def incorpEntityGrsCreateRequest(
    businessVerificationCheck: Boolean = true
  )(implicit request: Request[_]) =
    IncorpEntityGrsCreateRequest(grsCallbackUrl,
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink,
                                 businessVerificationCheck =
                                   businessVerificationCheck
    )

  private def getUkCompanyRedirectUrl(
    businessVerificationCheck: Boolean
  )(implicit request: Request[_], headerCarrier: HeaderCarrier): Future[String] =
    ukCompanyGrsConnector.createJourney(incorpEntityGrsCreateRequest(businessVerificationCheck))

  private def getRegisteredSocietyRedirectUrl()(implicit
    request: Request[_],
    headerCarrier: HeaderCarrier
  ): Future[String] =
    registeredSocietyGrsConnector.createJourney(incorpEntityGrsCreateRequest())

  private def getRedirectUrl(url: String, businessVerificationCheck: Boolean)(implicit
    request: JourneyRequest[AnyContent],
    headerCarrier: HeaderCarrier
  ): Future[String] =
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(appConfig.grsCallbackUrl,
                                  Some(request2Messages(request)("service.name")),
                                  appConfig.serviceIdentifier,
                                  appConfig.signOutLink,
                                  businessVerificationCheck =
                                    businessVerificationCheck
      ),
      url
    )

  private def updateRegistration(partnershipType: PartnershipTypeEnum)(implicit
    req: JourneyRequest[AnyContent],
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.organisationDetails.partnershipDetails match {
        case Some(_) =>
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(partnershipDetails =
              Some(
                registration.organisationDetails.partnershipDetails.get.copy(partnershipType =
                  partnershipType
                )
              )
            )
          )
        case _ =>
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(partnershipDetails =
              Some(PartnershipDetails(partnershipType = partnershipType))
            )
          )
      }
    }

}
