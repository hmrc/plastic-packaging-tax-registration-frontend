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

import play.api.i18n.I18nSupport
import play.api.mvc.Results.{Redirect, SeeOther}
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.{AppConfig, Features}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors._
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  RegisteredSocietyGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{OrgType, OrganisationType}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorpEntityGrsCreateRequest,
  SoleTraderGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest

import scala.concurrent.{ExecutionContext, Future}

trait OrganisationDetailsTypeHelper extends Cacheable with I18nSupport {

  def soleTraderGrsConnector: SoleTraderGrsConnector
  def ukCompanyGrsConnector: UkCompanyGrsConnector
  def registeredSocietyGrsConnector: RegisteredSocietyGrsConnector
  def registrationConnector: RegistrationConnector
  def appConfig: AppConfig

  protected def handleOrganisationType(organisationType: OrganisationType)(implicit
    request: JourneyRequest[AnyContent],
    executionContext: ExecutionContext,
    headerCarrier: HeaderCarrier
  ) =
    (organisationType.answer, request.isFeatureFlagEnabled(Features.isUkCompanyPrivateBeta)) match {
      case (Some(OrgType.UK_COMPANY), _) =>
        getUkCompanyRedirectUrl()
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.SOLE_TRADER), false) =>
        getSoleTraderRedirectUrl()
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.REGISTERED_SOCIETY), false) =>
        getRegisteredSocietyRedirectUrl()
          .map(journeyStartUrl => SeeOther(journeyStartUrl).addingToSession())
      case (Some(OrgType.PARTNERSHIP), false) =>
        // TODO - if this is a group registration then `Partnership` means `Limited liability partnership` so "partnership type" question not needed
        Future(Redirect(pptRoutes.PartnershipTypeController.displayPage()))
      case _ =>
        Future(Redirect(pptRoutes.OrganisationTypeNotSupportedController.onPageLoad()))
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
                                 appConfig.externalSignOutLink
      )
    )

  private def incorpEntityGrsCreateRequest(implicit request: Request[_]) =
    IncorpEntityGrsCreateRequest(grsCallbackUrl,
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.externalSignOutLink
    )

  private def getUkCompanyRedirectUrl()(implicit
    request: Request[_],
    headerCarrier: HeaderCarrier
  ): Future[String] =
    ukCompanyGrsConnector.createJourney(incorpEntityGrsCreateRequest)

  private def getRegisteredSocietyRedirectUrl()(implicit
    request: Request[_],
    headerCarrier: HeaderCarrier
  ): Future[String] =
    registeredSocietyGrsConnector.createJourney(incorpEntityGrsCreateRequest)

}