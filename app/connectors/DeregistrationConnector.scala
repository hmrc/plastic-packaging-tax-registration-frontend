/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import config.AppConfig
import models.deregistration.DeregistrationDetails
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeregistrationConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, metrics: Metrics)(implicit
  ec: ExecutionContext
) {

  def deregister(pptReference: String, deregistrationDetails: DeregistrationDetails)(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, Unit]] = {
    val timer = metrics.defaultRegistry.timer("ppt.deregister.timer").time()
    httpClient
      .put(url"${appConfig.pptSubscriptionDeregisterUrl(pptReference)}")
      .withBody(Json.toJson(deregistrationDetails.reason))
      .execute[HttpResponse]
      .andThen { case _ =>
        timer.stop()
      }
      .map {
        case response @ HttpResponse(OK, _, _) =>
          Right(())
        case response =>
          Left(
            DownstreamServiceError(
              s"Failed to de-register, status: ${response.status}, error: ${response.body}",
              FailedToDeregister("Failed to de-register")
            )
          )
      }.recover { case ex: Exception =>
        Left(DownstreamServiceError(s"Error while de-registration, error: ${ex.getMessage}", ex))
      }
  }

}

case class FailedToDeregister(errorMessage: String) extends Exception
