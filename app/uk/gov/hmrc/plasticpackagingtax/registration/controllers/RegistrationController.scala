/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  registration_group,
  registration_single_entity
}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class RegistrationController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  singleEntityPage: registration_single_entity,
  groupPage: registration_group
) extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(registrationPage(request.registration, startLink))
      if (request.registration.isGroup)
        Ok(groupPage(request.registration))
      else
        Ok(singleEntityPage(request.registration))
    }

  private def startLink(implicit request: JourneyRequest[AnyContent]) =
    if (request.isFeatureFlagEnabled(Features.isPreLaunch))
      routes.LiabilityWeightExpectedController.displayPage()
    else routes.LiabilityWeightController.displayPage()

}
