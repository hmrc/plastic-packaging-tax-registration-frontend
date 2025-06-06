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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.JourneyAction
import controllers.amendment.AmendmentController
import services.AmendRegistrationService
import views.html.amendment.partner.amend_partner_contact_check_answers_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnerContactDetailsCheckAnswersController @Inject() (
  journeyAction: JourneyAction,
  amendRegistrationService: AmendRegistrationService,
  mcc: MessagesControllerComponents,
  page: amend_partner_contact_check_answers_page
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendRegistrationService) {

  def displayPage(partnerId: String): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      Ok(
        page(
          request.registration.findPartner(partnerId).getOrElse(throw new IllegalStateException("Partner not found"))
        )
      )
    }

  def submit(): Action[AnyContent] =
    journeyAction.amend {
      Redirect(routes.PartnersListController.displayPage())
    }

}
