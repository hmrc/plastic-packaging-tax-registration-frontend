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

package controllers

import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import controllers.actions.NotEnrolledAuthAction
import controllers.actions.getRegistration.GetRegistrationAction
import controllers.liability.{
  routes => liabilityRoutes
}
import models.request.JourneyRequest
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class StartRegistrationController @Inject() (
                                              authenticate: NotEnrolledAuthAction,
                                              journeyAction: GetRegistrationAction,
                                              mcc: MessagesControllerComponents
) extends FrontendController(mcc) {

  def startRegistration(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>

      if (request.registration.isStarted) {
        Redirect(routes.TaskListController.displayPage()).addingToSession(
          "resumePPTRegistration" -> "true"
        )
      } else {
        Redirect(startLink).addingToSession("resumePPTRegistration" -> "true") //todo whats this????????????
        //gaaaaaahhhhhhhhhhhhhhhhhhhhh
      }
    }

  def startLink(implicit request: JourneyRequest[AnyContent]): Call =
    liabilityRoutes.ExpectToExceedThresholdWeightController.displayPage()

}
