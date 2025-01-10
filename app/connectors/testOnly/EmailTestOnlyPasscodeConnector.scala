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

package connectors.testOnly

import config.AppConfig
import connectors.{DownstreamServiceError, ServiceError}
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailTestOnlyPasscodeConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit
                                                                                               ec: ExecutionContext
) {

  def getTestOnlyPasscode()(implicit hc: HeaderCarrier): Future[Either[ServiceError, String]] =
    httpClient
      .get(url"${appConfig.getTestOnlyPasscodeUrl}")
      .execute[HttpResponse]
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
