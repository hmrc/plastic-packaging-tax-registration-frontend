/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.grs

import models.genericregistration.GrsJourneyCreationRequest
import play.api.Logger
import play.api.http.Status.CREATED
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, InternalServerException, StringContextOps}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}

abstract class GrsConnector[GrsCreateJourneyPayload <: GrsJourneyCreationRequest[
  GrsCreateJourneyPayload
], GrsResponse, TranslatedResponse](
  httpClient: HttpClientV2,
  metrics: Metrics,
  val grsCreateJourneyUrl: Option[String],
  val grsGetDetailsUrl: String,
  createJourneyTimerTag: String,
  getJourneyDetailsTimerTag: String
)(implicit ec: ExecutionContext) {
  type RedirectUrl = String

  private val logger = Logger(this.getClass)

  def createJourney(
    payload: GrsCreateJourneyPayload
  )(implicit wts: Writes[GrsCreateJourneyPayload], hc: HeaderCarrier): Future[RedirectUrl] =
    create(grsCreateJourneyUrl.getOrElse(throw new IllegalStateException("No url is specified")), payload)

  def createJourney(payload: GrsCreateJourneyPayload, grsCreateJourneyUrl: String)(implicit
    wts: Writes[GrsCreateJourneyPayload],
    hc: HeaderCarrier
  ): Future[RedirectUrl] =
    create(grsCreateJourneyUrl, payload)

  def getDetails(
    journeyId: String
  )(implicit rds: HttpReads[GrsResponse], hc: HeaderCarrier): Future[TranslatedResponse] = {
    val timerCtx = metrics.defaultRegistry.timer(getJourneyDetailsTimerTag).time()
    httpClient
      .get(url"$grsGetDetailsUrl/$journeyId")
      .execute[GrsResponse]
      .map(translateDetails)
      .andThen { case _ => timerCtx.stop() }
  }

  def translateDetails(grsResponse: GrsResponse): TranslatedResponse

  private def create(url: String, payload: GrsCreateJourneyPayload)(implicit
    wts: Writes[GrsCreateJourneyPayload],
    hc: HeaderCarrier
  ): Future[RedirectUrl] = {
    val timerCtx = metrics.defaultRegistry.timer(createJourneyTimerTag).time()
    httpClient
      .post(url"$url")
      .withBody(Json.toJson(payload.setBusinessVerificationCheckFalse))
      .execute[HttpResponse]
      .andThen { case _ =>
        timerCtx.stop()
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
