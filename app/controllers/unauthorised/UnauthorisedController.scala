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

package controllers.unauthorised

import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.unauthorised.{unauthorised, unauthorised_agent, unauthorised_not_admin}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject

class UnauthorisedController @Inject() (
  mcc: MessagesControllerComponents,
  unauthorisedPage: unauthorised,
  unauthorisedNotAdminPage: unauthorised_not_admin,
  unauthorisedAgent: unauthorised_agent
) extends FrontendController(mcc) with I18nSupport {

  private val logger = Logger(this.getClass)

  def showGenericUnauthorised(): Action[AnyContent] =
    Action { implicit request =>
      Ok(unauthorisedPage())
    }

  def showAssistantUnauthorised(): Action[AnyContent] =
    Action { implicit request =>
      logger.info("User with 'Assistant' credential role - showing unauthorised page")
      Ok(unauthorisedNotAdminPage())
    }

  def showAgentUnauthorised(): Action[AnyContent] =
    Action { implicit request =>
      logger.info("Agent attempting registration - showing unauthorised page")
      Ok(unauthorisedAgent())
    }
}
