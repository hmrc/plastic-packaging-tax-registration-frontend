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
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  SoleTraderIncorpIdCreateRequest,
  SoleTraderIncorporationDetails
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SoleTraderInorpIdConnector @Inject() (
  httpClient: HttpClient,
  config: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext) {

  def createJourney(
    payload: SoleTraderIncorpIdCreateRequest
  )(implicit hc: HeaderCarrier): Future[String] = {
    val timer = metrics.defaultRegistry.timer("ppt.soleTrader.incorpId.create.journey.timer").time()
    httpClient.POST[SoleTraderIncorpIdCreateRequest, HttpResponse](config.soleTraderJourneyUrl,
                                                                   payload
    )
      .andThen { case _ => timer.stop() }
      .map {
        case response @ HttpResponse(CREATED, _, _) =>
          (response.json \ "journeyStartUrl").as[String]
        case response =>
          throw new InternalServerException(
            s"Invalid response from sole trader entity identification: Status: ${response.status} Body: ${response.body}"
          )
      }
  }

  def getDetails(
    journeyId: String
  )(implicit hc: HeaderCarrier): Future[SoleTraderIncorporationDetails] = {
    val timer = metrics.defaultRegistry.timer("ppt.soleTrader.incorpId.get.details.timer").time()
    httpClient.GET[JsValue](config.soleTraderDetailsUrl(journeyId))
      .andThen { case _ => timer.stop() }
      .map { json =>
        SoleTraderIncorporationDetails.apiFormat.reads(json) match {
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw new Exception(
              s"Sole trader entity identification returned invalid JSON ${errors.map(_._1).mkString(", ")}"
            )
        }
      }
  }

}
