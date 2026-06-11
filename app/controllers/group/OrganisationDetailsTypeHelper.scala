/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.group

import play.api.i18n.I18nSupport
import play.api.mvc.Results.{Redirect, SeeOther}
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import config.AppConfig
import connectors.grs.{PartnershipGrsConnector, RegisteredSocietyGrsConnector, SoleTraderGrsConnector, UkCompanyGrsConnector}
import controllers.organisation.{routes => organisationRoutes}
import controllers.partner.{routes => partnerRoutes}
import forms.organisation.{OrgType, OrganisationType}
import forms.organisation.PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
import models.genericregistration.{IncorpEntityGrsCreateRequest, PartnershipDetails, PartnershipGrsCreateRequest, SoleTraderGrsCreateRequest}
import models.registration.RegistrationUpdater
import models.request.JourneyRequest

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
    organisationType.answer match {
      case Some(OrgType.UK_COMPANY) =>
        getIncorpRedirectResultOrResume(
          orgType = OrgType.UK_COMPANY,
          createJourney = getUkCompanyRedirectUrl(businessVerificationCheck, memberId),
          memberId = memberId
        )
      case Some(OrgType.OVERSEAS_COMPANY_UK_BRANCH) =>
        getIncorpRedirectResultOrResume(
          orgType = OrgType.OVERSEAS_COMPANY_UK_BRANCH,
          createJourney = getUkCompanyRedirectUrl(businessVerificationCheck, memberId),
          memberId = memberId
        )
      case Some(OrgType.SOLE_TRADER) =>
        getSoleTraderRedirectResultOrResume(memberId)
      case Some(OrgType.REGISTERED_SOCIETY) =>
        getIncorpRedirectResultOrResume(
          orgType = OrgType.REGISTERED_SOCIETY,
          createJourney = getRegisteredSocietyRedirectUrl(memberId),
          memberId = memberId
        )
      case Some(OrgType.PARTNERSHIP) =>
        getPartnershipRedirectUrl(memberId, businessVerificationCheck)
      case _ =>
        Future(Redirect(organisationRoutes.RegisterAsOtherOrganisationController.onPageLoad()))
    }

  def grsCallbackUrl(organisationId: Option[String] = None): String

  private def getPartnershipRedirectUrl(memberId: Option[String], businessVerificationCheck: Boolean)(implicit
    request: JourneyRequest[AnyContent],
    headerCarrier: HeaderCarrier,
    executionContext: ExecutionContext
  ): Future[Result] =
    if (request.registration.isGroup)
      getPartnershipRedirectResultOrResume(
        createJourney =
          getRedirectUrl(appConfig.limitedLiabilityPartnershipJourneyUrl, businessVerificationCheck, memberId),
        resumeUrl = appConfig.partnershipCompanyRegistrationNumberUrl,
        memberId = memberId
      )
    else
      Future(Redirect(partnerRoutes.PartnershipTypeController.displayPage()))

  private def getSoleTraderRedirectUrl(
    memberId: Option[String]
  )(implicit request: JourneyRequest[AnyContent], headerCarrier: HeaderCarrier): Future[String] =
    soleTraderGrsConnector.createJourney(
      SoleTraderGrsCreateRequest(
        grsCallbackUrl(memberId),
        Some(request2Messages(request)("service.name")),
        appConfig.serviceIdentifier,
        appConfig.signOutLink,
        appConfig.grsAccessibilityStatementPath
      )
    )

  private def getSoleTraderRedirectResultOrResume(memberId: Option[String])(implicit
    request: JourneyRequest[AnyContent],
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier
  ): Future[Result] =
    request.registration.incorpJourneyId match {
      case Some(journeyId)
          if isMainOrganisationJourney(memberId) &&
            request.registration.organisationDetails.organisationType.contains(OrgType.SOLE_TRADER) =>
        Future.successful(SeeOther(appConfig.soleTraderFullNameUrl(journeyId)))
      case _ =>
        getSoleTraderRedirectUrl(memberId).flatMap { journeyStartUrl =>
          persistOrganisationJourneyOrRedirect(journeyStartUrl, memberId, extractSoleTraderJourneyId) { journeyId =>
            _.copy(
              incorpJourneyId = Some(journeyId),
              organisationDetails =
                request.registration.organisationDetails.copy(organisationType = Some(OrgType.SOLE_TRADER))
            )
          }
        }
    }

  private def incorpEntityGrsCreateRequest(businessVerificationCheck: Boolean, memberId: Option[String])(implicit
    request: Request[_]
  ) =
    IncorpEntityGrsCreateRequest(
      grsCallbackUrl(memberId),
      Some(request2Messages(request)("service.name")),
      appConfig.serviceIdentifier,
      appConfig.signOutLink,
      appConfig.grsAccessibilityStatementPath,
      businessVerificationCheck = businessVerificationCheck
    )

  private def getUkCompanyRedirectUrl(businessVerificationCheck: Boolean, memberId: Option[String])(implicit
    request: Request[_],
    headerCarrier: HeaderCarrier
  ): Future[String] =
    ukCompanyGrsConnector.createJourney(incorpEntityGrsCreateRequest(businessVerificationCheck, memberId))

  private def getIncorpRedirectResultOrResume(
    orgType: OrgType,
    createJourney: => Future[String],
    memberId: Option[String]
  )(implicit
    request: JourneyRequest[AnyContent],
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier
  ): Future[Result] =
    request.registration.incorpJourneyId match {
      case Some(journeyId)
          if isMainOrganisationJourney(memberId) &&
            request.registration.organisationDetails.organisationType.contains(orgType) =>
        Future.successful(SeeOther(appConfig.incorpCompanyNumberUrl(journeyId)))
      case _ =>
        createJourney.flatMap { journeyStartUrl =>
          persistOrganisationJourneyOrRedirect(journeyStartUrl, memberId, extractJourneyId) { journeyId =>
            _.copy(
              incorpJourneyId = Some(journeyId),
              organisationDetails = request.registration.organisationDetails.copy(organisationType = Some(orgType))
            )
          }
        }
    }

  private def getPartnershipRedirectResultOrResume(
    createJourney: => Future[String],
    resumeUrl: String => String,
    memberId: Option[String]
  )(implicit
    request: JourneyRequest[AnyContent],
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier
  ): Future[Result] =
    request.registration.incorpJourneyId match {
      case Some(journeyId)
          if isMainOrganisationJourney(memberId) &&
            request.registration.organisationDetails.organisationType.contains(OrgType.PARTNERSHIP) &&
            request.registration.organisationDetails.partnershipDetails.exists(
              _.partnershipType == LIMITED_LIABILITY_PARTNERSHIP
            ) =>
        Future.successful(SeeOther(resumeUrl(journeyId)))
      case _ =>
        createJourney.flatMap { journeyStartUrl =>
          persistOrganisationJourneyOrRedirect(journeyStartUrl, memberId, extractPartnershipJourneyId) {
            journeyId => registration =>
              registration.copy(
                incorpJourneyId = Some(journeyId),
                organisationDetails = registration.organisationDetails.copy(
                  organisationType = Some(OrgType.PARTNERSHIP),
                  partnershipDetails = Some(
                    registration.organisationDetails.partnershipDetails
                      .getOrElse(PartnershipDetails(LIMITED_LIABILITY_PARTNERSHIP))
                      .copy(partnershipType = LIMITED_LIABILITY_PARTNERSHIP)
                  )
                )
              )
          }
        }
    }

  private def persistOrganisationJourneyOrRedirect(
    journeyStartUrl: String,
    memberId: Option[String],
    extractJourneyId: String => Option[String]
  )(updateRegistrationModel: String => models.registration.Registration => models.registration.Registration)(implicit
    request: JourneyRequest[AnyContent],
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier
  ): Future[Result] =
    if (isMainOrganisationJourney(memberId))
      extractJourneyId(journeyStartUrl).fold(Future.successful(SeeOther(journeyStartUrl))) { journeyId =>
        registrationUpdater
          .updateRegistration(updateRegistrationModel(journeyId))
          .map(_ => SeeOther(journeyStartUrl))
      }
    else
      Future.successful(SeeOther(journeyStartUrl))

  private def isMainOrganisationJourney(memberId: Option[String])(implicit
    request: JourneyRequest[AnyContent]
  ): Boolean =
    memberId.isEmpty && request.registration.groupDetail.forall(_.currentMemberOrganisationType.isEmpty)

  private def extractJourneyId(journeyStartUrl: String): Option[String] =
    """.*/identify-your-incorporated-business/([^/]+)/company-number$""".r
      .findFirstMatchIn(journeyStartUrl)
      .map(_.group(1))
      .orElse(
        """/identify-your-incorporated-business/([^/]+)/company-number$""".r
          .findFirstMatchIn(journeyStartUrl)
          .map(_.group(1))
      )

  private def extractSoleTraderJourneyId(journeyStartUrl: String): Option[String] =
    """.*/identify-your-sole-trader-business/([^/]+)/full-name$""".r
      .findFirstMatchIn(journeyStartUrl)
      .map(_.group(1))
      .orElse(
        """/identify-your-sole-trader-business/([^/]+)/full-name$""".r
          .findFirstMatchIn(journeyStartUrl)
          .map(_.group(1))
      )

  private def extractPartnershipJourneyId(journeyStartUrl: String): Option[String] =
    """.*/identify-your-partnership/([^/]+)/(?:company-registration-number|sa-utr)$""".r
      .findFirstMatchIn(journeyStartUrl)
      .map(_.group(1))
      .orElse(
        """/identify-your-partnership/([^/]+)/(?:company-registration-number|sa-utr)$""".r
          .findFirstMatchIn(journeyStartUrl)
          .map(_.group(1))
      )

  private def getRegisteredSocietyRedirectUrl(
    memberId: Option[String]
  )(implicit request: Request[_], headerCarrier: HeaderCarrier): Future[String] =
    registeredSocietyGrsConnector.createJourney(incorpEntityGrsCreateRequest(true, memberId))

  private def getRedirectUrl(url: String, businessVerificationCheck: Boolean, memberId: Option[String])(implicit
    request: JourneyRequest[AnyContent],
    headerCarrier: HeaderCarrier
  ): Future[String] =
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(
        grsCallbackUrl(memberId),
        Some(request2Messages(request)("service.name")),
        appConfig.serviceIdentifier,
        appConfig.signOutLink,
        appConfig.grsAccessibilityStatementPath,
        businessVerificationCheck = businessVerificationCheck
      ),
      url
    )

}
