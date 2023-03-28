/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.testOnly

import play.api.i18n.I18nSupport
import play.api.mvc._
import connectors.testOnly.EmailTestOnlyPasscodeConnector
import controllers.actions.PermissiveAuthActionImpl
import models.request.JourneyAction
import models.testOnly.EmailPasscode
import play.api.{Environment, Mode}
import play.api.libs.json.{JsArray, Json, OWrites}
import repositories.testOnly.{JourneyMongoRepository, PasscodeMongoRepository}
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailPasscodeController @Inject() (
                                          authenticate: PermissiveAuthActionImpl,
                                          mcc: MessagesControllerComponents,
                                          journeyAction: JourneyAction,
                                          journeyRepo: JourneyMongoRepository,
                                          passCodeRep: PasscodeMongoRepository,
                                          environment: Environment,
                                          emailTestOnlyPasscodeConnector: EmailTestOnlyPasscodeConnector
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def testOnlyGetPasscodes(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      emailTestOnlyPasscodeConnector.getTestOnlyPasscode.flatMap {
        case Right(response) => Future.successful(Ok(response))
        case Left(error)     => throw error
      }
    }

  def testOnlyGetAllPasscodes(): Action[AnyContent] =
    authenticate.async { implicit request =>

      if (isInProductionMode) throw new RuntimeException("Wrong environment")
      else {

        hc.sessionId match {
          case Some(SessionId(id)) =>
            for {
              passcodeDocs <- passCodeRep.findPasscodesBySessionId(id)
              journeyDocs <- journeyRepo.findAll
              emailPasscodes = passcodeDocs.map(d => EmailPasscode(d.email, d.passcode)) ++
                journeyDocs.filter(_.emailAddress.isDefined).map(d => EmailPasscode(d.emailAddress.get, d.passcode))
            } yield Ok(Json.obj {
              "passcodes" -> JsArray(emailPasscodes.map {
                Json.toJson(_)
              })
            })
          case None =>
            Future.successful(Unauthorized(Json.toJson(ErrorResponse("NO_SESSION_ID", "No session id provided"))))
        }
      }
    }

  private def isInProductionMode = environment.mode == Mode.Prod

}

case class ErrorResponse(code: String, message: String, details: Option[Map[String, String]] = None)

object ErrorResponse {
  implicit val writes: OWrites[ErrorResponse] = Json.writes[ErrorResponse]
}
