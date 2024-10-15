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
import connectors.PPTReference.pptReferenceTimer
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class PPTReference(clientPPT: String)

object ClientRegistration {
  implicit val format: OFormat[PPTReference] = Json.format[PPTReference]
}

@Singleton
class PptReferenceConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig, metrics: Metrics)(implicit
  ec: ExecutionContext
) {
  import ClientRegistration._
  def get(implicit hc: HeaderCarrier): Future[String] = {
    val url   = appConfig.pptReferenceUrl
    val timer = metrics.defaultRegistry.timer(pptReferenceTimer).time()
    httpClient.GET[PPTReference](url = url)
      .andThen { case _ => timer.stop() }
      .recover { case exception: Exception =>
        throw DownstreamServiceError(
          s"Failed to get ppt reference number",
          exception
        )
      }
  }.map(_.clientPPT)
}

object PPTReference {
  val pptReferenceTimer = "ppt.reference.timer"
}
