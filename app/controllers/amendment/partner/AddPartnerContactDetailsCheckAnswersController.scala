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

package controllers.amendment.partner

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.EnrolledAuthAction
import controllers.amendment.{AmendmentController, routes => amendRoutes}
import models.request.AmendmentJourneyAction
import models.subscriptions.{
  SubscriptionCreateOrUpdateResponseFailure,
  SubscriptionCreateOrUpdateResponseSuccess
}
import views.html.amendment.partner.amend_add_partner_contact_check_answers_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerContactDetailsCheckAnswersController @Inject() (
                                                                 authenticate: EnrolledAuthAction,
                                                                 journeyAction: AmendmentJourneyAction,
                                                                 mcc: MessagesControllerComponents,
                                                                 page: amend_add_partner_contact_check_answers_page
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, journeyAction) {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(page(request.registration.inflightPartner.getOrElse(throw new IllegalStateException("Missing partner"))))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      journeyAction.updateRegistration(_ => request.registration.withPromotedInflightPartner()).map {
        case _: SubscriptionCreateOrUpdateResponseSuccess =>
          Redirect(routes.ManagePartnersController.displayPage())
        case _: SubscriptionCreateOrUpdateResponseFailure =>
          Redirect(amendRoutes.AmendRegistrationController.registrationUpdateFailed())
      }.recover {
        case _: Exception =>
          Redirect(amendRoutes.AmendRegistrationController.registrationUpdateFailed())
      }
    }

}