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

import connectors.RegistrationConnector
import controllers.actions.JourneyAction
import controllers.liability.{routes => liabilityRoutes}
import models.registration.Cacheable
import models.request.JourneyRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.{task_list_group, task_list_partnership, task_list_single_entity}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TaskListController @Inject()(
                                    journeyAction: JourneyAction,
                                    mcc: MessagesControllerComponents,
                                    singleEntityPage: task_list_single_entity,
                                    groupPage: task_list_group,
                                    partnershipPage: task_list_partnership,
                                    override val registrationConnector: RegistrationConnector
)
  (implicit ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with Cacheable {

  def displayPage(): Action[AnyContent] = {
    journeyAction.register { implicit request =>

      val hasOldLiabilityQuestions: Boolean = request.registration.hasOldLiabilityQuestions
      if (hasOldLiabilityQuestions)
        update { _ => request.registration.clearOldLiabilityAnswers }

      if (request.registration.isGroup)
        Ok(groupPage(request.registration, liabilityStartLink, hasOldLiabilityQuestions))
      else if (request.registration.isPartnershipWithPartnerCollection)
        Ok(partnershipPage(request.registration, liabilityStartLink, hasOldLiabilityQuestions))
      else
        Ok(singleEntityPage(request.registration, liabilityStartLink, hasOldLiabilityQuestions))
    }
  }

  private def liabilityStartLink(implicit request: JourneyRequest[AnyContent]): Call = {

    if(request.registration.isLiabilityDetailsComplete)
      liabilityRoutes.CheckLiabilityDetailsAnswersController.displayPage()
    else
      liabilityRoutes.ExpectToExceedThresholdWeightController.displayPage()
  }
}
