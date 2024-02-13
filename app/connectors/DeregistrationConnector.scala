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

import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import play.api.http.Status.OK
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import config.AppConfig
import models.deregistration.DeregistrationDetails
import models.deregistration.DeregistrationReason.DeregistrationReason

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeregistrationConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig, metrics: Metrics)(implicit ec: ExecutionContext) {

  def deregister(pptReference: String, deregistrationDetails: DeregistrationDetails)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Unit]] = {
    val timer = metrics.defaultRegistry.timer("ppt.deregister.timer").time()
    httpClient.PUT[Option[DeregistrationReason], HttpResponse](appConfig.pptSubscriptionDeregisterUrl(pptReference), deregistrationDetails.reason).andThen {
      case _ => timer.stop()
    }
      .map {
        case response @ HttpResponse(OK, _, _) =>
          Right(())
        case response =>
          Left(DownstreamServiceError(s"Failed to de-register, status: ${response.status}, error: ${response.body}", FailedToDeregister("Failed to de-register")))
      }.recover {
        case ex: Exception =>
          Left(DownstreamServiceError(s"Error while de-registration, error: ${ex.getMessage}", ex))
      }
  }

}

case class FailedToDeregister(errorMessage: String) extends Exception
