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

import javax.inject.{Inject, Singleton}
import play.api.http.Status.CREATED
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  IncorpIdCreateRequest,
  IncorporationDetails
}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncorpIdConnector @Inject() (httpClient: HttpClient, config: AppConfig)(implicit
  ec: ExecutionContext
) {

  def createJourney(payload: IncorpIdCreateRequest)(implicit hc: HeaderCarrier): Future[String] =
    httpClient.POST[IncorpIdCreateRequest, HttpResponse](config.incorpJourneyUrl, payload).map {
      case response @ HttpResponse(CREATED, _, _) =>
        (response.json \ "journeyStartUrl").as[String]
      case response =>
        throw new InternalServerException(
          s"Invalid response from incorporated entity identification: Status: ${response.status} Body: ${response.body}"
        )
    }

  def getDetails(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporationDetails] =
    httpClient.GET[JsValue](config.incorpDetailsUrl(journeyId))
      .map { json =>
        IncorporationDetails.apiFormat.reads(json) match {
          case JsSuccess(value, _) => value
          case JsError(errors) =>
            throw new Exception(
              s"Incorporated entity identification returned invalid JSON ${errors.map(_._1).mkString(", ")}"
            )
        }
      }

}
