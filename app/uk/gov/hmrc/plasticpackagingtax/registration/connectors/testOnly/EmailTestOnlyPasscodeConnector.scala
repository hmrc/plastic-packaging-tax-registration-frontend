/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors.testOnly

import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  DownstreamServiceError,
  ServiceError
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailTestOnlyPasscodeConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)(
  implicit ec: ExecutionContext
) {

  def getTestOnlyPasscode()(implicit hc: HeaderCarrier): Future[Either[ServiceError, String]] =
    httpClient.GET[HttpResponse](appConfig.getTestOnlyPasscodeUrl)
      .map {
        case response @ HttpResponse(OK, _, _) =>
          Right(response.body)
        case response =>
          Left(
            DownstreamServiceError(
              s"Unable to find test only passcodes :, status: ${response.status}, error: ${response.body}",
              FailedToFetchTestOnlyPasscode("Failed to get test only passcodes")
            )
          )
      }

}

case class FailedToFetchTestOnlyPasscode(message: String) extends Exception
