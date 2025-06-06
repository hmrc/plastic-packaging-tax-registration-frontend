/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.auth.BasicAuthAction
import controllers.actions.getRegistration.GetRegistrationAction
import controllers.group.{routes => groupRoutes}
import views.html.group.{group_member_already_registered_page, nominated_organisation_already_registered_page, organisation_already_in_group_page}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class NotableErrorController @Inject() (
  authenticate: BasicAuthAction,
  getRegistration: GetRegistrationAction,
  mcc: MessagesControllerComponents,
  nominatedOrganisationAlreadyRegisteredPage: nominated_organisation_already_registered_page,
  organisationAlreadyInGroupPage: organisation_already_in_group_page,
  groupMemberAlreadyRegisteredPage: group_member_already_registered_page
) extends FrontendController(mcc)
    with I18nSupport {

  def nominatedOrganisationAlreadyRegistered(): Action[AnyContent] =
    (authenticate andThen getRegistration) { implicit request =>
      Ok(nominatedOrganisationAlreadyRegisteredPage())
    }

  def organisationAlreadyInGroup(): Action[AnyContent] =
    (authenticate andThen getRegistration) { implicit request =>
      request.registration.groupDetail.flatMap(_.groupError) match {
        case Some(groupError) => Ok(organisationAlreadyInGroupPage(groupError))
        case _                => Redirect(groupRoutes.OrganisationListController.displayPage())
      }
    }

  def groupMemberAlreadyRegistered(): Action[AnyContent] =
    (authenticate andThen getRegistration) { implicit request =>
      request.registration.groupDetail.flatMap(_.groupError) match {
        case Some(groupError) => Ok(groupMemberAlreadyRegisteredPage(groupError))
        case _                => Redirect(groupRoutes.OrganisationListController.displayPage())
      }
    }

}
