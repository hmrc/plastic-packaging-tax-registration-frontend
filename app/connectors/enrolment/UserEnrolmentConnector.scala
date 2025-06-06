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

package connectors.enrolment

import config.AppConfig
import connectors.enrolment.UserEnrolmentConnector.UserEnrolmentTimer
import models.enrolment._
import models.registration.UserEnrolmentDetails
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class UserEnrolmentConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig, metrics: Metrics)(implicit
  ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  def enrol(payload: UserEnrolmentDetails)(implicit hc: HeaderCarrier): Future[UserEnrolmentResponse] = {
    val timer = metrics.defaultRegistry.timer(UserEnrolmentTimer).time()
    httpClient
      .post(url"${appConfig.pptEnrolmentUrl}")
      .withBody(Json.toJson(payload.toUserEnrolmentRequest))
      .execute[HttpResponse]
      .andThen { case _ => timer.stop() }
      .map { enrolmentResponse =>
        logger.info(
          s"PPT enrol user for PPT reference [${payload.pptReference.getOrElse("")}] had response status [${enrolmentResponse.status}] payload [${enrolmentResponse.body}]"
        )
        if (Status.isSuccessful(enrolmentResponse.status))
          Try(enrolmentResponse.json.as[UserEnrolmentSuccessResponse]) match {
            case Success(userEnrolmentSuccessResponse) => userEnrolmentSuccessResponse
            case Failure(e) =>
              throw new IllegalStateException(s"Unexpected successful user enrolment response - ${e.getMessage}", e)
          }
        else
          Try(enrolmentResponse.json.as[UserEnrolmentFailedResponse]) match {
            case Success(userEnrolmentFailedResponse) => userEnrolmentFailedResponse
            case Failure(e) =>
              throw new IllegalStateException(s"Unexpected failed user enrolment response - ${e.getMessage}", e)
          }
      }
  }

}

object UserEnrolmentConnector {
  val UserEnrolmentTimer = "ppt.user.enrolment.timer"
}
