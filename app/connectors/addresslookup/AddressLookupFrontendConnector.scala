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

package connectors.addresslookup

import config.AppConfig
import models.addresslookup.{AddressLookupConfigV2, AddressLookupConfirmation, AddressLookupOnRamp}
import play.api.http.HeaderNames.LOCATION
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupFrontendConnector @Inject() (http: HttpClientV2, appConfig: AppConfig, metrics: Metrics) {

  def initialiseJourney(
    addressLookupRequest: AddressLookupConfigV2
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AddressLookupOnRamp] = {
    val timer = metrics.defaultRegistry.timer("ppt.addresslookup.initialise.timer").time()
    http
      .post(url"${appConfig.addressLookupInitUrl}")
      .withBody(Json.toJson(addressLookupRequest))
      .execute[HttpResponse]
      .andThen { case _ =>
        timer.stop()
      }
      .map {
        case response @ HttpResponse(ACCEPTED, _, _) =>
          response.header(LOCATION) match {
            case Some(redirectUrl) => AddressLookupOnRamp(redirectUrl)
            case _                 => throw new IllegalStateException("Missing re-direct url")
          }
        case error =>
          throw new IllegalStateException(s"Error. Address look up frontend error with status: ${error.status}")
      }
  }

  def getAddress(id: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AddressLookupConfirmation] = {
    val timer = metrics.defaultRegistry.timer("ppt.addresslookup.getaddress.timer").time()
    http
      .get(url"${appConfig.addressLookupConfirmedUrl}?id=$id")
      .execute[AddressLookupConfirmation]
      .andThen { case _ => timer.stop() }
  }

}
