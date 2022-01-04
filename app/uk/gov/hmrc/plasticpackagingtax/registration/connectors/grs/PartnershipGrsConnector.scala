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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs

import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.GeneralPartnershipGrsConnector.{
  CreateJourneyTimer,
  GetDetailsTimer
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  GrsPartnershipBusinessDetails,
  PartnershipBusinessDetails,
  PartnershipGrsCreateRequest
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnershipGrsConnector @Inject() (
  httpClient: HttpClient,
  config: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext)
    extends GrsConnector[
      PartnershipGrsCreateRequest,
      GrsPartnershipBusinessDetails,
      PartnershipBusinessDetails
    ](httpClient,
      metrics,
      None,
      config.partnershipJourneyUrl,
      CreateJourneyTimer,
      GetDetailsTimer
    ) {

  def translateDetails(
    grsPartnershipBusinessDetails: GrsPartnershipBusinessDetails
  ): PartnershipBusinessDetails =
    PartnershipBusinessDetails(grsPartnershipBusinessDetails)

}

object GeneralPartnershipGrsConnector {
  val CreateJourneyTimer = "ppt.partnership.create.journey.timer"
  val GetDetailsTimer    = "ppt.partnership.get.details.timer"
}
