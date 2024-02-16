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

package controllers.testOnly

import config.AppConfig
import controllers.testOnly.html.SkipAuthView
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.util.UUID
import javax.inject.Inject

class SkipAuthStubController @Inject() (val controllerComponents: ControllerComponents, appConfig: AppConfig, skipAuthView: SkipAuthView)
    extends BaseController with FrontendHeaderCarrierProvider with Logging with I18nSupport {

  def returns(): Action[AnyContent] =
    Action { implicit request =>
      val params: Map[String, Seq[String]] = Map(
        "authorityId"                         -> "",
        "redirectionUrl"                      -> appConfig.pptAccountUrl,
        "credentialStrength"                  -> "strong",
        "confidenceLevel"                     -> "50",
        "affinityGroup"                       -> "Organisation",
        "enrolment[0].name"                   -> "HMRC-PPT-ORG",
        "enrolment[0].taxIdentifier[0].name"  -> "EtmpRegistrationNumber",
        "enrolment[0].taxIdentifier[0].value" -> "XMPPT0000000003",
        "enrolment[0].state"                  -> "Activated"
      ).map { case (key, default) => key -> request.queryString.getOrElse(key, Seq(default)) }

      Ok(skipAuthView("Returns", params))
    }

  def registration(): Action[AnyContent] =
    Action { implicit request =>
      val params: Map[String, Seq[String]] = Map(
        "authorityId"        -> UUID.randomUUID().toString,
        "redirectionUrl"     -> appConfig.loginContinueUrl,
        "credentialStrength" -> "strong",
        "confidenceLevel"    -> "50",
        "affinityGroup"      -> "Organisation"
      ).map { case (key, default) => key -> request.queryString.getOrElse(key, Seq(default)) }
      logger.info("PPT_UR user CredId(authorityId)= " + params("authorityId").head)
      Ok(skipAuthView("Registration", params))
    }

}
