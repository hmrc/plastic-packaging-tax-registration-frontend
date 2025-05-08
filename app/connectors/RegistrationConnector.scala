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

package connectors

import config.AppConfig
import models.registration.Registration
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, metrics: Metrics)(implicit
  ec: ExecutionContext
) {

  def find(id: String)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Option[Registration]]] = {
    val timer = metrics.defaultRegistry.timer("ppt.registration.find.timer").time()
    httpClient
      .get(url"${appConfig.pptRegistrationUrl(id)}")
      .execute[Option[Registration]]
      .andThen { case _ => timer.stop() }
      .map(resp => Right(resp.map(_.toRegistration)))
      .recover { case ex: Exception =>
        Left(DownstreamServiceError(s"Failed to retrieve registration, error: ${ex.getMessage}", ex))
      }
  }

  def create(payload: Registration)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Registration]] = {
    val timer = metrics.defaultRegistry.timer("ppt.registration.create.timer").time()
    httpClient
      .post(url"${appConfig.pptRegistrationUrl}")
      .withBody(Json.toJson(payload))
      .execute[Registration]
      .andThen { case _ => timer.stop() }
      .map(response => Right(response.toRegistration))
      .recover { case ex: Exception =>
        Left(DownstreamServiceError(s"Failed to create registration, error: ${ex.getMessage}", ex))
      }
  }

  def update(payload: Registration)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Registration]] = {
    val timer = metrics.defaultRegistry.timer("ppt.registration.update.timer").time()
    httpClient
      .put(url"${appConfig.pptRegistrationUrl(payload.id)}")
      .withBody(Json.toJson(payload))
      .execute[Registration]
      .andThen { case _ => timer.stop() }
      .map(response => Right(response.toRegistration))
      .recover { case ex: Exception =>
        Left(DownstreamServiceError(s"Failed to update registration, error: ${ex.getMessage}", ex))
      }
  }

}
