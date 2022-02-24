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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.SessionRecordNotFound
import uk.gov.hmrc.mongo.cache.{DataKey, MongoCacheRepository}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserDataRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class KeepAliveController @Inject() (
  mcc: MessagesControllerComponents,
  userDataRepository: UserDataRepository,
  authenticate: AuthNoEnrolmentCheckAction
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  private val logger = Logger(this.getClass)

  def keepAlive(): Action[AnyContent] =
    authenticate { implicit request =>
      implicit val cif = MongoCacheRepository.format
      request.session.get("sessionId") match {
        case Some(sessionId) =>
          userDataRepository.findBySessionId(sessionId).map {
            values =>
              values match {
                case Some(cacheItem) =>
                  cacheItem.data.fields.headOption match {
                    case Some(keyValuePair) =>
                      userDataRepository.put[JsValue](sessionId)(DataKey(keyValuePair._1),
                                                                 keyValuePair._2
                      )
                    case None => throw SessionRecordNotFound()
                  }
                case _ => throw SessionRecordNotFound()
              }
              Ok(request.uri)
          }
        case _ =>
          logger.warn(s"Denied attempt to access ${request.uri} since no user session present")
          throw SessionRecordNotFound()
      }
      Ok(request.uri)
    }

}
