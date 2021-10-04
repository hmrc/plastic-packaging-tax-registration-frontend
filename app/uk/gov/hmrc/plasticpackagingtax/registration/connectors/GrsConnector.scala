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
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, InternalServerException}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

abstract class GrsConnector[
  GrsCreateJourneyPayload,
  GrsResponse
] @Inject() (
  httpClient: HttpClient,
  metrics: Metrics,
  grsUrl: String,
  createJourneyTimerTag: String,
  getJourneyDetailsTimerTag: String
)
(implicit ec: ExecutionContext) {
  type RedirectUrl  = String
  type GrsJourneyId = String

  def createJourney(payload: GrsCreateJourneyPayload)(implicit wts: Writes[GrsCreateJourneyPayload], hc: HeaderCarrier): Future[RedirectUrl] = {
    val timerCtx = metrics.defaultRegistry.timer(createJourneyTimerTag).time()
    httpClient.POST[GrsCreateJourneyPayload, HttpResponse](grsUrl, payload)
      .andThen { case _ => timerCtx.stop() }
      .map {
        case response@HttpResponse(CREATED, _, _) =>
          (response.json \ "journeyStartUrl").as[String]
        case response =>
          throw new InternalServerException(
            s"Invalid response from GRS: Status: ${response.status} Body: ${response.body}"
          )
      }
  }

  def getDetails(journeyId: String)(implicit rds: Reads[GrsResponse], hc: HeaderCarrier): Future[GrsResponse] = {
    val timerCtx = metrics.defaultRegistry.timer(getJourneyDetailsTimerTag).time()
    httpClient.GET[GrsResponse](s"$grsUrl/$journeyId")
      .andThen { case _ => timerCtx.stop() }
  }
}

