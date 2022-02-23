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

package uk.gov.hmrc.plasticpackagingtax.registration.models.request

import com.google.inject.ImplementedBy
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.auth.core.SessionRecordNotFound
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserDataRepository
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class KeepAliveActionImpl @Inject() (appConfig: AppConfig, userDataRepository: UserDataRepository)(
  implicit val exec: ExecutionContext
) extends KeepAliveAction {

  private val logger = Logger(this.getClass)

  protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, JourneyRequest[A]]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    request.session.get("sessionId") match {
      case Some(sessionId) =>
        userDataRepository.findAll[Registration](sessionId).map {
          values =>
            for (value <- values) {
              val key: String = value match {
                case JsObject(jsValue) => jsValue.keys.head
                case _                 => "None"
              }
              userDataRepository.put[JsValue](sessionId)(DataKey(key), value)
            }
            Right(JourneyRequest[A](request, values.head.as[Registration], appConfig))
        }
      case _ =>
        logger.warn(s"Denied attempt to access ${request.uri} since no user session present")
        throw SessionRecordNotFound()
    }

  }

  override protected def executionContext: ExecutionContext = exec
}

@ImplementedBy(classOf[KeepAliveActionImpl])
trait KeepAliveAction extends ActionRefiner[AuthenticatedRequest, JourneyRequest]
