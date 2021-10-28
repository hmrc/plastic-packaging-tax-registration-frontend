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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors.enrolment

import com.kenshoo.play.metrics.Metrics
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.enrolment.UserEnrolmentConnector.UserEnrolmentTimer
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.UserEnrolmentDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class UserEnrolmentConnector @Inject() (
  httpClient: HttpClient,
  appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext) {

  def enrol(
    payload: UserEnrolmentDetails
  )(implicit hc: HeaderCarrier): Future[UserEnrolmentResponse] = {
    val timer = metrics.defaultRegistry.timer(UserEnrolmentTimer).time()
    httpClient.POST[UserEnrolmentRequest, HttpResponse](appConfig.pptEnrolmentUrl,
                                                        payload.toUserEnrolmentRequest
    )
      .andThen { case _ => timer.stop() }
      .map {
        enrolmentResponse =>
          if (Status.isSuccessful(enrolmentResponse.status))
            Try(enrolmentResponse.json.as[UserEnrolmentSuccessResponse]) match {
              case Success(userEnrolmentSuccessResponse) => userEnrolmentSuccessResponse
              case Failure(e) =>
                throw new IllegalStateException(
                  s"Unexpected successful user enrolment response - ${e.getMessage}",
                  e
                )
            }
          else
            Try(enrolmentResponse.json.as[UserEnrolmentFailedResponse]) match {
              case Success(userEnrolmentFailedResponse) => userEnrolmentFailedResponse
              case Failure(e) =>
                throw new IllegalStateException(
                  s"Unexpected failed user enrolment response - ${e.getMessage}",
                  e
                )
            }
      }
  }

}

object UserEnrolmentConnector {
  val UserEnrolmentTimer = "ppt.user.enrolment.timer"
}
