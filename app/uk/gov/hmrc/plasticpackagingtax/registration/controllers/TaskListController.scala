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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.NotEnrolledAuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{routes => liabilityRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{task_list_group, task_list_partnership, task_list_single_entity}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TaskListController @Inject()(
  authenticate: NotEnrolledAuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  singleEntityPage: task_list_single_entity,
  groupPage: task_list_group,
  partnershipPage: task_list_partnership,
  override val registrationConnector: RegistrationConnector
)
  (implicit ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with Cacheable {

  def displayPage(): Action[AnyContent] = {
    (authenticate andThen journeyAction) { implicit request =>

      val startLink = liabilityRoutes.ExpectToExceedThresholdWeightController.displayPage()
      val hasOldLiabilityQuestions: Boolean = request.registration.hasOldLiabilityQuestions
      if (hasOldLiabilityQuestions)
        update { _ => request.registration.clearOldLiabilityAnswers }

      if (request.registration.isGroup)
        Ok(groupPage(request.registration, startLink, hasOldLiabilityQuestions))
      else if (request.registration.isPartnershipWithPartnerCollection)
        Ok(partnershipPage(request.registration, startLink, hasOldLiabilityQuestions))
      else
        Ok(singleEntityPage(request.registration, startLink, hasOldLiabilityQuestions))
    }
  }

}
