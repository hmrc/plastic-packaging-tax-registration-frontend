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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs

import com.kenshoo.play.metrics.Metrics
import play.api.Logger
import play.api.http.Status.CREATED
import play.api.libs.json.Writes
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpReads,
  HttpResponse,
  InternalServerException
}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.grs.ScottishPartnershipGrsConnector.{
  CreateJourneyTimer,
  GetDetailsTimer
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  GrsLimitedPartnershipDetails,
  LimitedPartnershipDetails,
  PartnershipGrsCreateRequest
}

import scala.concurrent.{ExecutionContext, Future}

abstract class PartnershipGrsConnector(
  httpClient: HttpClient,
  config: AppConfig,
  metrics: Metrics,
  override val grsCreateJourneyUrl: String
)(implicit ec: ExecutionContext)
    extends GrsConnector[
      PartnershipGrsCreateRequest,
      GrsLimitedPartnershipDetails,
      LimitedPartnershipDetails
    ](httpClient,
      metrics,
      grsCreateJourneyUrl,
      config.partnershipJourneyUrl,
      CreateJourneyTimer,
      GetDetailsTimer
    ) {

  def translateDetails(
    grsLimitedPartnershipDetails: GrsLimitedPartnershipDetails
  ): LimitedPartnershipDetails =
    LimitedPartnershipDetails(grsLimitedPartnershipDetails)

}
