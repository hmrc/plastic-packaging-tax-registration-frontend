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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.AmendmentController
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.member_contact_check_answers_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ContactDetailsCheckAnswersController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  amendmentJourneyAction: AmendmentJourneyAction,
  mcc: MessagesControllerComponents,
  page: member_contact_check_answers_page
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendmentJourneyAction) {

  def displayPage(memberId: String): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      Ok(
        page(
          request.registration.groupDetail.flatMap(_.findGroupMember(memberId)).getOrElse(
            throw new IllegalStateException("Missing group member")
          )
        )
      )
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      Redirect(routes.GroupMembersListController.displayPage())
    }

}