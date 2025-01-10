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
import models.registration.Registration
import models.subscriptions.{SubscriptionCreateOrUpdateResponse, SubscriptionCreateOrUpdateResponseFailure, SubscriptionCreateOrUpdateResponseSuccess, SubscriptionStatusResponse}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SubscriptionsConnector @Inject() (httpClient: HttpClientV2, config: AppConfig, metrics: Metrics)(implicit
                                                                                                      ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  def getSubscriptionStatus(safeId: String)(implicit hc: HeaderCarrier): Future[SubscriptionStatusResponse] = {
    val timer = metrics.defaultRegistry.timer("ppt.subscription.status.timer").time()
    httpClient
      .get(url"${config.pptSubscriptionStatusUrl(safeId)}")
      .execute[SubscriptionStatusResponse]
      .andThen { case _ => timer.stop() }
      .andThen {
        case Success(response) =>
          logger.info(s"PPT get subscription status for safeId [$safeId] had response payload [${toJson(response)}]")
          response
        case Failure(exception) =>
          throw new Exception(
            s"Subscription Status with Safe ID [$safeId] is currently unavailable due to [${exception.getMessage}]",
            exception
          )
      }
  }

  def getSubscription(pptReference: String)(implicit hc: HeaderCarrier): Future[Registration] = {
    val timer = metrics.defaultRegistry.timer("ppt.subscription.get.timer").time()
    httpClient
      .get(url"${config.pptSubscriptionGetUrl(pptReference)}")
      .execute[Registration]
      .andThen { case _ => timer.stop() }
      .andThen {
        case Success(registration) =>
          logger.info(s"Successfully obtained PPT subscription for pptReference [$pptReference]")
          registration
        case Failure(exception) =>
          throw new Exception(
            s"Failed to obtain PPT subscription for pptReference [$pptReference] due to [${exception.getMessage}]",
            exception
          )
      }
  }

  def updateSubscription(pptReference: String, registration: Registration)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionCreateOrUpdateResponse] = {
    val timer = metrics.defaultRegistry.timer("ppt.subscription.update.timer").time()
    httpClient
      .put(url"${config.pptSubscriptionUpdateUrl(pptReference)}")
      .withBody(Json.toJson(registration))
      .execute[HttpResponse]
      .andThen { case _ => timer.stop() }
      .map { updateSubscriptionResponse =>
        logger.info(
          s"PPT update subscription for pptReference [$pptReference] had response status " +
            s"[${updateSubscriptionResponse.status}] and payload [${updateSubscriptionResponse.body}]"
        )
        if (Status.isSuccessful(updateSubscriptionResponse.status))
          Try(updateSubscriptionResponse.json.as[SubscriptionCreateOrUpdateResponseSuccess]) match {
            case Success(successfulSubscription) => successfulSubscription
            case Failure(e) =>
              throw new IllegalStateException(s"Unexpected successful subscription response - ${e.getMessage}", e)
          }
        else
          Try(updateSubscriptionResponse.json.as[SubscriptionCreateOrUpdateResponseFailure]) match {
            case Success(failedSubscription) => failedSubscription
            case Failure(e) =>
              logger.warn("Failed to update subscription - invalid response payload received")
              throw new IllegalStateException(s"Unexpected failed subscription response - ${e.getMessage}", e)
          }
      }
  }

  def submitSubscription(safeId: String, payload: Registration)(implicit
    hc: HeaderCarrier
  ): Future[SubscriptionCreateOrUpdateResponse] = {
    val timer = metrics.defaultRegistry.timer("ppt.subscription.submit.timer").time()
    httpClient
      .post(url"${config.pptSubscriptionCreateUrl(safeId)}")
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]
      .andThen { case _ => timer.stop() }
      .map { createSubscriptionResponse =>
        logger.info(
          s"PPT submit subscription for safeId [$safeId] had response status " +
            s"[${createSubscriptionResponse.status}] and payload [${createSubscriptionResponse.body}]"
        )
        if (Status.isSuccessful(createSubscriptionResponse.status))
          Try(createSubscriptionResponse.json.as[SubscriptionCreateOrUpdateResponseSuccess]) match {
            case Success(successfulSubscription) => successfulSubscription
            case Failure(e) =>
              throw new IllegalStateException(s"Unexpected successful subscription response - ${e.getMessage}", e)
          }
        else
          Try(createSubscriptionResponse.json.as[SubscriptionCreateOrUpdateResponseFailure]) match {
            case Success(failedSubscription) => failedSubscription
            case Failure(e) =>
              throw new IllegalStateException(s"Unexpected failed subscription response - ${e.getMessage}", e)
          }
      }
  }

}
