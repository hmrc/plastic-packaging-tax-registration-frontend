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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  CreateRegistrationRequest,
  Registration
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationConnector @Inject() (
  httpClient: HttpClient,
  appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext) {

  def find(
    id: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, Option[Registration]]] = {
    val timer = metrics.defaultRegistry.timer("ppt.find.registration.timer").time()
    httpClient.GET[Option[Registration]](appConfig.pptRegistrationUrl(id))
      .andThen { case _ => timer.stop() }
      .map(resp => Right(resp.map(_.toRegistration)))
      .recover {
        case ex: Exception =>
          Left(
            DownstreamServiceError(s"Failed to retrieve registration, error: ${ex.getMessage}", ex)
          )
      }
  }

  def create(
    payload: CreateRegistrationRequest
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, Registration]] = {
    val timer = metrics.defaultRegistry.timer("ppt.create.registration.timer").time()
    httpClient.POST[CreateRegistrationRequest, Registration](appConfig.pptRegistrationUrl, payload)
      .andThen { case _ => timer.stop() }
      .map(response => Right(response.toRegistration))
      .recover {
        case ex: Exception =>
          Left(
            DownstreamServiceError(s"Failed to create registration, error: ${ex.getMessage}", ex)
          )
      }
  }

  def update(
    payload: Registration
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, Registration]] = {
    val timer = metrics.defaultRegistry.timer("ppt.update.registration.timer").time()
    httpClient.PUT[Registration, Registration](appConfig.pptRegistrationUrl(payload.id), payload)
      .andThen { case _ => timer.stop() }
      .map(response => Right(response.toRegistration))
      .recover {
        case ex: Exception =>
          Left(
            DownstreamServiceError(s"Failed to update registration, error: ${ex.getMessage}", ex)
          )
      }
  }

}
