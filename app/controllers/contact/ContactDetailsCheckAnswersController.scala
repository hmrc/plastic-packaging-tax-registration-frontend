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

package controllers.contact

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.NotEnrolledAuthAction
import controllers.{routes => commonRoutes}
import models.request.JourneyAction
import views.html.contact.check_primary_contact_details_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class ContactDetailsCheckAnswersController @Inject() (
                                                       authenticate: NotEnrolledAuthAction,
                                                       journeyAction: JourneyAction,
                                                       mcc: MessagesControllerComponents,
                                                       page: check_primary_contact_details_page
) extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(page(request.registration))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction) { _ =>
      Redirect(commonRoutes.TaskListController.displayPage())
    }

}