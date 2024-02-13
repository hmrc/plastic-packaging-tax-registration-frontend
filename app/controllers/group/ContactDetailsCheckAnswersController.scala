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

package controllers.group

import connectors.RegistrationConnector
import controllers.actions.JourneyAction
import models.registration.Cacheable
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.group.member_contact_check_answers_page

import javax.inject.{Inject, Singleton}

@Singleton
class ContactDetailsCheckAnswersController @Inject() (
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: member_contact_check_answers_page
) extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(memberId: String): Action[AnyContent] =
    journeyAction.register { implicit request =>
      Ok(page(request.registration.groupDetail.flatMap(_.findGroupMember(Some(memberId), None)).getOrElse(throw new IllegalStateException("Missing group member"))))
    }

  def submit(): Action[AnyContent] =
    journeyAction.register { _ =>
      Redirect(routes.OrganisationListController.displayPage())
    }

}
