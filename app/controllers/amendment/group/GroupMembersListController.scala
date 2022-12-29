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

package controllers.amendment.group

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.EnrolledAuthAction
import controllers.amendment.group
import forms.group.AddOrganisationForm
import models.request.AmendmentJourneyAction
import views.html.amendment.group.list_group_members_page
import views.viewmodels.ListGroupMembersViewModel
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class GroupMembersListController @Inject() (
                                             authenticate: EnrolledAuthAction,
                                             amendmentJourneyAction: AmendmentJourneyAction,
                                             mcc: MessagesControllerComponents,
                                             page: list_group_members_page
) extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      Ok(page(AddOrganisationForm.form(), new ListGroupMembersViewModel(request.registration)))
    }

  def onSubmit(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      AddOrganisationForm
        .form()
        .bindFromRequest()
        .fold(error => BadRequest(page(error, new ListGroupMembersViewModel(request.registration))),
              add =>
                if (add)
                  Redirect(
                    group.routes.AddGroupMemberOrganisationDetailsTypeController.displayPage()
                  )
                else
                  Redirect(routes.ManageGroupMembersController.displayPage())
        )
    }

}
