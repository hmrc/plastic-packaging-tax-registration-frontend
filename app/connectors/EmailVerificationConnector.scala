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
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import config.AppConfig
import models.emailverification.EmailVerificationJourneyStatus.{COMPLETE, INCORRECT_PASSCODE, JOURNEY_NOT_FOUND, JourneyStatus, TOO_MANY_ATTEMPTS}
import models.emailverification.{CreateEmailVerificationRequest, VerificationStatus, VerifyPasscodeRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig, metrics: Metrics)(implicit
  ec: ExecutionContext
) {

  def getStatus(id: String)(implicit hc: HeaderCarrier): Future[Either[ServiceError, Option[VerificationStatus]]] = {
    val timer = metrics.defaultRegistry.timer("ppt.email.verification.getStatus.timer").time()
    httpClient.GET[Option[VerificationStatus]](appConfig.getEmailVerificationStatusUrl(id))
      .andThen { case _ => timer.stop() }
      .map(resp => Right(resp.map(_.toVerificationStatus)))
      .recover { case ex: Exception =>
        Left(DownstreamServiceError(s"Failed to retrieve email verification status, error: ${ex.getMessage}", ex))
      }
  }

  def create(
    payload: CreateEmailVerificationRequest
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, String]] = {
    val timer = metrics.defaultRegistry.timer("ppt.email.verification.create.timer").time()
    httpClient.POST[CreateEmailVerificationRequest, HttpResponse](appConfig.emailVerificationUrl, payload)
      .andThen { case _ => timer.stop() }
      .map {
        case response @ HttpResponse(CREATED, _, _) =>
          Right((response.json \ "redirectUri").as[String])
        case response =>
          Left(
            DownstreamServiceError(
              s"Failed to create email verification, status: ${response.status}, error: ${response.body}",
              CreateEmailVerificationException("Failed to create email verification")
            )
          )
      }.recover { case ex: Exception =>
        Left(DownstreamServiceError(s"Error while verifying email, error: ${ex.getMessage}", ex))
      }
  }

  def verifyPasscode(journeyId: String, payload: VerifyPasscodeRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, JourneyStatus]] = {
    val timer = metrics.defaultRegistry.timer("ppt.email.verification.verify.passcode.timer").time()
    httpClient.POST[VerifyPasscodeRequest, HttpResponse](
      appConfig.getSubmitPassscodeUrl(journeyId = journeyId),
      payload
    )
      .andThen { case _ => timer.stop() }
      .map {
        case _ @HttpResponse(OK, _, _) =>
          Right(COMPLETE)
        case _ @HttpResponse(BAD_REQUEST, _, _) =>
          Right(INCORRECT_PASSCODE)
        case _ @HttpResponse(FORBIDDEN, _, _) =>
          Right(TOO_MANY_ATTEMPTS)
        case _ @HttpResponse(NOT_FOUND, _, _) =>
          Right(JOURNEY_NOT_FOUND)
        case response =>
          Left(
            DownstreamServiceError(
              s"Failed to verify passcode, status: ${response.status}, error: ${response.body}",
              VerifyPasscodeException("Failed to verify passcode")
            )
          )
      }.recover { case ex: Exception =>
        Left(DownstreamServiceError(s"Error while verifying passcode, error: ${ex.getMessage}", ex))
      }
  }

}

case class CreateEmailVerificationException(message: String) extends Exception
case class VerifyPasscodeException(message: String)          extends Exception
case class FailedToFetchTestOnlyPasscode(message: String)    extends Exception
