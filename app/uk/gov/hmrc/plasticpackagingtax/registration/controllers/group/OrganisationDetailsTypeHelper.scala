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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{
  OrgType,
  OrganisationType,
  PartnerTypeEnum
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
    businessVerificationCheck: Boolean = true,
    memberId: Option[String]
  )(implicit
    request: JourneyRequest[AnyContent],
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier
  ) =
    (organisationType.answer, request.isFeatureFlagEnabled(Features.isUkCompanyPrivateBeta)) match {
      case (Some(OrgType.UK_COMPANY), _) =>
        getUkCompanyRedirectUrl(businessVerificationCheck, memberId)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.OVERSEAS_COMPANY_UK_BRANCH), false) =>
        getUkCompanyRedirectUrl(businessVerificationCheck, memberId)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.SOLE_TRADER), false) =>
        getSoleTraderRedirectUrl(memberId)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.REGISTERED_SOCIETY), false) =>
        getRegisteredSocietyRedirectUrl(memberId)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.PARTNERSHIP), false) =>
        if (request.registration.isGroup) {
          updateRegistration(organisationType.answer, PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP)
          getRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl,
                         businessVerificationCheck,
                         memberId
          ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
        } else
          Future(Redirect(partnerRoutes.PartnershipTypeController.displayPage()))
      case _ =>
        Future(Redirect(organisationRoutes.OrganisationTypeNotSupportedController.onPageLoad()))
    }

  def grsCallbackUrl(organisationId: Option[String] = None): String

  private def getSoleTraderRedirectUrl(
    memberId: Option[String]
  )(implicit request: JourneyRequest[AnyContent], headerCarrier: HeaderCarrier): Future[String] =
    soleTraderGrsConnector.createJourney(
      SoleTraderGrsCreateRequest(grsCallbackUrl(memberId),
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink,
                                 appConfig.grsAccessibilityStatementPath
      )
    )

  private def incorpEntityGrsCreateRequest(
    businessVerificationCheck: Boolean = true,
    memberId: Option[String]
  )(implicit request: Request[_]) =
    IncorpEntityGrsCreateRequest(grsCallbackUrl(memberId),
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink,
                                 appConfig.grsAccessibilityStatementPath,
                                 businessVerificationCheck =
                                   businessVerificationCheck
    )

  private def getUkCompanyRedirectUrl(
    businessVerificationCheck: Boolean,
    memberId: Option[String]
  )(implicit request: Request[_], headerCarrier: HeaderCarrier): Future[String] =
    ukCompanyGrsConnector.createJourney(
      incorpEntityGrsCreateRequest(businessVerificationCheck, memberId)
    )

  private def getRegisteredSocietyRedirectUrl(
    memberId: Option[String]
  )(implicit request: Request[_], headerCarrier: HeaderCarrier): Future[String] =
    registeredSocietyGrsConnector.createJourney(incorpEntityGrsCreateRequest(true, memberId))

  private def getRedirectUrl(
    url: String,
    businessVerificationCheck: Boolean,
    memberId: Option[String]
  )(implicit request: JourneyRequest[AnyContent], headerCarrier: HeaderCarrier): Future[String] =
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(grsCallbackUrl(memberId),
                                  Some(request2Messages(request)("service.name")),
                                  appConfig.serviceIdentifier,
                                  appConfig.signOutLink,
                                  appConfig.grsAccessibilityStatementPath,
                                  businessVerificationCheck =
                                    businessVerificationCheck
      ),
      url
    )

  private def updateRegistration(
    organisationType: Option[OrgType],
    partnershipType: PartnerTypeEnum
  )(implicit
    req: JourneyRequest[AnyContent],
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.organisationDetails.partnershipDetails match {
        case Some(_) =>
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(
              partnershipDetails =
                Some(
                  registration.organisationDetails.partnershipDetails.get.copy(partnershipType =
                    partnershipType
                  )
                ),
              organisationType = organisationType
            )
          )
        case _ =>
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(
              partnershipDetails =
                Some(PartnershipDetails(partnershipType = partnershipType)),
              organisationType = organisationType
            )
          )
      }
    }

}
