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

package uk.gov.hmrc.plasticpackagingtax.registration.services

import play.api.i18n.I18nSupport
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.{
  PartnershipGrsConnector,
  RegisteredSocietyGrsConnector,
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

// TODO !Can be migrated from trait to a service to reduce dependency injections in callers
trait GRSRedirections extends I18nSupport {

  def appConfig: AppConfig
  def soleTraderGrsConnector: SoleTraderGrsConnector
  def ukCompanyGrsConnector: UkCompanyGrsConnector
  def partnershipGrsConnector: PartnershipGrsConnector
  def registeredSocietyGrsConnector: RegisteredSocietyGrsConnector

  def getUkCompanyRedirectUrl(grsUrl: String, callbackUrl: String)(implicit
    request: JourneyRequest[AnyContent],
    hc: HeaderCarrier
  ): Future[String] =
    ukCompanyGrsConnector.createJourney(incorpEntityGrsCreateRequest(callbackUrl), grsUrl)

  def getPartnershipRedirectUrl(
    grsUrl: String,
    callbackUrl: String,
    businessVerification: Boolean = true
  )(implicit request: JourneyRequest[AnyContent], hc: HeaderCarrier): Future[String] =
    partnershipGrsConnector.createJourney(
      PartnershipGrsCreateRequest(callbackUrl,
                                  Some(request2Messages(request)("service.name")),
                                  appConfig.serviceIdentifier,
                                  appConfig.signOutLink,
                                  appConfig.grsAccessibilityStatementPath,
                                  businessVerificationCheck = businessVerification
      ),
      grsUrl
    )

  def getSoleTraderRedirectUrl(grsUrl: String, callbackUrl: String)(implicit
    request: JourneyRequest[AnyContent],
    hc: HeaderCarrier
  ): Future[String] =
    soleTraderGrsConnector.createJourney(
      SoleTraderGrsCreateRequest(callbackUrl,
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink,
                                 appConfig.grsAccessibilityStatementPath,
                                 businessVerificationCheck = false
      ),
      grsUrl
    )

  def getRegisteredSocietyRedirectUrl(grsUrl: String, callbackUrl: String)(implicit
    request: JourneyRequest[_],
    headerCarrier: HeaderCarrier
  ): Future[String] =
    registeredSocietyGrsConnector.createJourney(incorpEntityGrsCreateRequest(callbackUrl), grsUrl)

  private def incorpEntityGrsCreateRequest(
    callbackUrl: String
  )(implicit request: JourneyRequest[_]) =
    IncorpEntityGrsCreateRequest(callbackUrl,
                                 Some(request2Messages(request)("service.name")),
                                 appConfig.serviceIdentifier,
                                 appConfig.signOutLink,
                                 appConfig.grsAccessibilityStatementPath,
                                 businessVerificationCheck =
                                   false
    )

}
