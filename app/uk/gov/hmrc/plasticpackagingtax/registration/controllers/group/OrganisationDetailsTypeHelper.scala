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
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.{AppConfig, Features}
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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{OrgType, OrganisationType}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorpEntityGrsCreateRequest,
  PartnershipGrsCreateRequest,
  SoleTraderGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.RegistrationUpdater
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest

import scala.concurrent.{ExecutionContext, Future}

trait OrganisationDetailsTypeHelper extends I18nSupport {

  def soleTraderGrsConnector: SoleTraderGrsConnector
  def ukCompanyGrsConnector: UkCompanyGrsConnector
  def partnershipGrsConnector: PartnershipGrsConnector
  def registeredSocietyGrsConnector: RegisteredSocietyGrsConnector
  def appConfig: AppConfig
  def registrationUpdater: RegistrationUpdater

  protected def handleOrganisationType(
    organisationType: OrganisationType,
    businessVerificationCheck: Boolean = true,
    memberId: Option[String]
  )(implicit
    request: JourneyRequest[AnyContent],
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier
  ): Future[Result] =
    (organisationType.answer, request.isFeatureFlagEnabled(Features.isUkCompanyPrivateBeta)) match {
      case (Some(OrgType.UK_COMPANY), _) =>
        getUkCompanyRedirectUrl(businessVerificationCheck, memberId)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.OVERSEAS_COMPANY_UK_BRANCH), _) =>
        getUkCompanyRedirectUrl(businessVerificationCheck, memberId)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.SOLE_TRADER), false) =>
        getSoleTraderRedirectUrl(memberId)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.REGISTERED_SOCIETY), false) =>
        getRegisteredSocietyRedirectUrl(memberId)
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.PARTNERSHIP), false) =>
        getPartnershipRedirectUrl(memberId, businessVerificationCheck)
      case _ =>
        Future(Redirect(organisationRoutes.RegisterAsOtherOrganisationController.onPageLoad()))
    }

  def grsCallbackUrl(organisationId: Option[String] = None): String

  private def getPartnershipRedirectUrl(
    memberId: Option[String],
    businessVerificationCheck: Boolean
  )(implicit
    request: JourneyRequest[AnyContent],
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Result] =
    request.isFeatureFlagEnabled(Features.isPartnershipEnabled) match {
      case true =>
        if (request.registration.isGroup)
          getRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl,
                         businessVerificationCheck,
                         memberId
          ).map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
        else
          Future(Redirect(partnerRoutes.PartnershipTypeController.displayPage()))
      case _ =>
        Future(Redirect(partnerRoutes.PartnerRegistrationAvailableSoonController.onPageLoad()))
    }

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

}
