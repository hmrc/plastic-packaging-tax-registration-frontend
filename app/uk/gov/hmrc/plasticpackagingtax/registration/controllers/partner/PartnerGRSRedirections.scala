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

import play.api.i18n.I18nSupport
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  PartnershipGrsConnector,
  SoleTraderGrsConnector,
  UkCompanyGrsConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorpEntityGrsCreateRequest,
  PartnershipGrsCreateRequest,
  SoleTraderGrsCreateRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest

import scala.concurrent.Future

trait PartnerGRSRedirections extends I18nSupport {

  def appConfig: AppConfig
  def soleTraderGrsConnector: SoleTraderGrsConnector
  def ukCompanyGrsConnector: UkCompanyGrsConnector
  def partnershipGrsConnector: PartnershipGrsConnector

  def getUkCompanyRedirectUrl(url: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent],
    hc: HeaderCarrier
  ): Future[String] =
    ukCompanyGrsConnector.createJourney(
      IncorpEntityGrsCreateRequest(appConfig.partnerGrsCallbackUrl(partnerId),
                                   Some(request2Messages(request)("service.name")),
                                   appConfig.serviceIdentifier,
                                   appConfig.signOutLink,
                                   appConfig.grsAccessibilityStatementPath,
                                   businessVerificationCheck = false
      ),
      url
    )

  def getPartnershipRedirectUrl(url: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent],
    hc: HeaderCarrier
  ): Future[String] =
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(appConfig.partnerGrsCallbackUrl(partnerId),
                                  Some(request2Messages(request)("service.name")),
                                  appConfig.serviceIdentifier,
                                  appConfig.signOutLink,
                                  appConfig.grsAccessibilityStatementPath
      ),
      url
    )

  def getSoleTraderRedirectUrl(url: String, partnerId: Option[String])(implicit
    request: JourneyRequest[AnyContent],
    hc: HeaderCarrier
  ): Future[String] =
    soleTraderGrsConnector.createJourney(
      SoleTraderGrsCreateRequest(appConfig.partnerGrsCallbackUrl(partnerId),
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink,
                                 appConfig.grsAccessibilityStatementPath
      ),
      url
    )

}
