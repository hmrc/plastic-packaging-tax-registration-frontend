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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  CreateEmailVerificationRequest,
  VerificationStatus
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationConnector @Inject() (
  httpClient: HttpClient,
  appConfig: AppConfig,
  metrics: Metrics
)(implicit ec: ExecutionContext) {

  def getStatus(
    id: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, VerificationStatus]] = {
    val timer = metrics.defaultRegistry.timer("ppt.email.verification.getStatus.timer").time()
    httpClient.GET[JsValue](appConfig.getEmailVerificationStatusUrl(id))
      .andThen { case _ => timer.stop() }
      .map { json =>
        VerificationStatus.apiReads.reads(json) match {
          case JsSuccess(value, _) => Right(value)
          case JsError(errors) =>
            Left(
              DownstreamServiceError(
                s"Failed to retrieve email verification status, error: ${errors.map(_._1).mkString(", ")}"
              )
            )
        }
      }
  }

  def create(
    payload: CreateEmailVerificationRequest
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, String]] = {
    val timer = metrics.defaultRegistry.timer("ppt.email.verification.create.timer").time()
    httpClient.POST[CreateEmailVerificationRequest, HttpResponse](appConfig.emailVerificationUrl,
                                                                  payload
    )
      .andThen { case _ => timer.stop() }
      .map {
        case response @ HttpResponse(CREATED, _, _) =>
          Right((response.json \ "redirectUri").as[String])
        case response =>
          Left(
            DownstreamServiceError(
              s"Failed to create email verification, status: ${response.status}, error: ${response.body}"
            )
          )
      }
  }

}
