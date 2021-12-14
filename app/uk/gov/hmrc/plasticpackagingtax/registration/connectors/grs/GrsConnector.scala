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

import scala.concurrent.{ExecutionContext, Future}

abstract class GrsConnector[GrsCreateJourneyPayload, GrsResponse, TranslatedResponse](
  httpClient: HttpClient,
  metrics: Metrics,
  val grsCreateJourneyUrl: String,
  val grsGetDetailsUrl: String,
  createJourneyTimerTag: String,
  getJourneyDetailsTimerTag: String
)(implicit ec: ExecutionContext) {
  type RedirectUrl = String

  private val logger = Logger(this.getClass)

  def createJourney(
    payload: GrsCreateJourneyPayload
  )(implicit wts: Writes[GrsCreateJourneyPayload], hc: HeaderCarrier): Future[RedirectUrl] =
    create(grsCreateJourneyUrl, payload)

  def createJourney(
    payload: GrsCreateJourneyPayload,
    grsCreateJourneyUrlForPartnership: String
  )(implicit wts: Writes[GrsCreateJourneyPayload], hc: HeaderCarrier): Future[RedirectUrl] =
    create(grsCreateJourneyUrlForPartnership, payload)

  def getDetails(
    journeyId: String
  )(implicit rds: HttpReads[GrsResponse], hc: HeaderCarrier): Future[TranslatedResponse] = {
    val timerCtx = metrics.defaultRegistry.timer(getJourneyDetailsTimerTag).time()
    httpClient.GET[GrsResponse](s"$grsGetDetailsUrl/$journeyId").map(translateDetails(_))
      .andThen { case _ => timerCtx.stop() }
  }

  def translateDetails(grsResponse: GrsResponse): TranslatedResponse

  private def create(url: String, payload: GrsCreateJourneyPayload)(implicit
    wts: Writes[GrsCreateJourneyPayload],
    hc: HeaderCarrier
  ): Future[RedirectUrl] = {
    val timerCtx = metrics.defaultRegistry.timer(createJourneyTimerTag).time()
    httpClient.POST[GrsCreateJourneyPayload, HttpResponse](url, payload)
      .andThen {
        case _ => timerCtx.stop()
      }
      .map {
        case response @ HttpResponse(CREATED, _, _) =>
          val url = (response.json \ "journeyStartUrl").as[String]
          logger.info(s"PPT starting GRS journey with url [$url]")
          url
        case response =>
          throw new InternalServerException(
            s"Invalid response from GRS: Status: ${response.status} Body: ${response.body}"
          )
      }
  }

}
