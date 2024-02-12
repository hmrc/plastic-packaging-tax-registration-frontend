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
import forms.group.AddOrganisationForm.form
import models.registration.Cacheable
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.group.organisation_list

import javax.inject.{Inject, Singleton}

@Singleton
class OrganisationListController @Inject() (
                                             journeyAction: JourneyAction,
                                             override val registrationConnector: RegistrationConnector,
                                             mcc: MessagesControllerComponents,
                                             page: organisation_list
) extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.groupDetail.fold(
        Redirect(controllers.routes.TaskListController.displayPage())
      )(
        detail =>
          if (detail.members.isEmpty)
            // Must add at least one group member to use this page
            Redirect(
              controllers.group.routes.OrganisationDetailsTypeController.displayPageNewMember()
            )
          else
            Ok(
              page(form(),
                   request.registration.organisationDetails.businessName.get,
                   detail.members
              )
            )
      )
    }

  def submit(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      form().bindFromRequest().fold(
        (formWithErrors: Form[Boolean]) =>
          BadRequest(
            page(formWithErrors,
                 request.registration.organisationDetails.businessName.get,
                 request.registration.groupDetail.map(_.members).get
            )
          ),
        addOrganisation =>
          if (addOrganisation)
            Redirect(controllers.group.routes.OrganisationDetailsTypeController.displayPageNewMember())
          else
            Redirect(controllers.routes.TaskListController.displayPage())
      )

    }

}
