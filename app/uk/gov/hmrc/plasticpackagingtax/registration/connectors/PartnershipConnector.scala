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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors

import com.kenshoo.play.metrics.Metrics
import play.api.http.Status.CREATED
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.PartnershipConnector.CreateJourneyTimer
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  PartnershipCreateJourneyRequest,
  PartnershipDetails
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipConnector @Inject() (httpClient: HttpClient, config: AppConfig, metrics: Metrics)(
  implicit ec: ExecutionContext
) extends GrsIntegration[PartnershipCreateJourneyRequest, PartnershipDetails] {

  override def createJourney(
    payload: PartnershipCreateJourneyRequest
  )(implicit hc: HeaderCarrier): Future[RedirectUrl] = {
    val timer = metrics.defaultRegistry.timer(CreateJourneyTimer).time()
    httpClient.POST[PartnershipCreateJourneyRequest, HttpResponse](
      config.generalPartnershipJourneyUrl,
      payload
    )
      .andThen { case _ => timer.stop() }
      .map {
        case response @ HttpResponse(CREATED, _, _) =>
          (response.json \ "journeyStartUrl").as[RedirectUrl]
        case response =>
          throw new InternalServerException(
            s"Invalid response from partnership entity identification: Status: ${response.status} Body: ${response.body}"
          )
      }
  }

  override def getDetails(
    journeyId: GrsJourneyId
  )(implicit hc: HeaderCarrier): Future[PartnershipDetails] =
    httpClient.GET[PartnershipDetails](config.partnershipDetailsUrl(journeyId))

}

object PartnershipConnector {
  val CreateJourneyTimer = "ppt.partnership.create.journey.timer"
}
