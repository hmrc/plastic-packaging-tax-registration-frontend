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

import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.prelaunch.{
  routes => prelaunchLiabilityRoutes
}

import javax.inject.{Inject, Singleton}

@Singleton
class StartRegistrationController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents
) extends FrontendController(mcc) {

  def startRegistration(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      if (request.registration.isStarted)
        Redirect(routes.TaskListController.displayPage()).addingToSession(
          "resumePPTRegistration" -> "true"
        )
      else
        Redirect(startLink).addingToSession("resumePPTRegistration" -> "true")
    }

  def startLink(implicit request: JourneyRequest[AnyContent]): Call =
    if (request.isFeatureFlagEnabled(Features.isPreLaunch))
      prelaunchLiabilityRoutes.LiabilityWeightExpectedController.displayPage()
    else liabilityRoutes.ExceededThresholdWeightController.displayPage()

}
