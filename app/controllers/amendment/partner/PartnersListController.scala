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

package controllers.amendment.partner

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.JourneyAction
import forms.group.AddOrganisationForm
import views.html.amendment.partner.list_partners_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class PartnersListController @Inject() (
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: list_partners_page
) extends FrontendController(mcc)
    with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      Ok(page(AddOrganisationForm.form(), request.registration))
    }

  def submit(): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      AddOrganisationForm
        .form()
        .bindFromRequest()
        .fold(
          error => BadRequest(page(error, request.registration)),
          add =>
            if (add)
              Redirect(routes.AddPartnerOrganisationDetailsTypeController.displayPage())
            else
              Redirect(routes.ManagePartnersController.displayPage())
        )
    }

}
