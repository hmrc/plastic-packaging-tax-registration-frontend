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
import play.api.http.Status
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  SubscriptionCreateResponse,
  SubscriptionCreateResponseFailure,
  SubscriptionCreateResponseSuccess,
  SubscriptionStatusResponse
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SubscriptionsConnector @Inject() (
  httpClient: HttpClient,
  config: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext) {

  def getSubscriptionStatus(
    safeNumber: String
  )(implicit hc: HeaderCarrier): Future[SubscriptionStatusResponse] = {
    val timer = metrics.defaultRegistry.timer("ppt.subscription.status.timer").time()
    httpClient.GET[SubscriptionStatusResponse](config.pptSubscriptionStatusUrl(safeNumber))
      .andThen { case _ => timer.stop() }
      .andThen {
        case Success(response) => response
        case Failure(exception) =>
          throw new Exception(
            s"Subscription Status with Safe ID [${safeNumber}] is currently unavailable due to [${exception.getMessage}]",
            exception
          )
      }
  }

  def submitSubscription(safeNumber: String, payload: Registration)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionCreateResponse] = {
    val timer = metrics.defaultRegistry.timer("ppt.subscription.submit.timer").time()
    httpClient.POST[Registration, HttpResponse](config.pptSubscriptionCreateUrl(safeNumber),
                                                payload
    )
      .andThen { case _ => timer.stop() }
      .map {
        subscriptionResponse =>
          if (Status.isSuccessful(subscriptionResponse.status))
            Try(subscriptionResponse.json.as[SubscriptionCreateResponseSuccess]) match {
              case Success(successfulSubscription) => successfulSubscription
              case Failure(e) =>
                throw new IllegalStateException(
                  s"Unexpected successful subscription response - ${e.getMessage}",
                  e
                )
            }
          else
            Try(subscriptionResponse.json.as[SubscriptionCreateResponseFailure]) match {
              case Success(failedSubscription) => failedSubscription
              case Failure(e) =>
                throw new IllegalStateException(
                  s"Unexpected failed subscription response - ${e.getMessage}",
                  e
                )
            }
      }
  }

}
