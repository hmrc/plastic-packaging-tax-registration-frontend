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

package controllers.amendment.group

import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import controllers.actions.EnrolledAuthAction
import controllers.amendment.AmendmentController
import controllers.amendment.{routes => amendRoutes}
import models.request.AmendmentJourneyAction
import models.subscriptions.{
  SubscriptionCreateOrUpdateResponseFailure,
  SubscriptionCreateOrUpdateResponseSuccess
}
import views.html.amendment.group.amend_member_contact_check_answers_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddGroupMemberContactDetailsCheckAnswersController @Inject() (
                                                                     authenticate: EnrolledAuthAction,
                                                                     journeyAction: AmendmentJourneyAction,
                                                                     mcc: MessagesControllerComponents,
                                                                     page: amend_member_contact_check_answers_page
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, journeyAction) {

  def displayPage(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      Ok(
        page(
          request.registration.groupDetail.flatMap(_.findGroupMember(Some(memberId), None))
            .getOrElse(throw new IllegalStateException("Missing group member")),
          getBackLink(memberId),
          getSubmitCall(memberId)
        )
      )
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      journeyAction.updateRegistration().map {
        case _: SubscriptionCreateOrUpdateResponseSuccess =>
          Redirect(routes.ManageGroupMembersController.displayPage())
        case _: SubscriptionCreateOrUpdateResponseFailure =>
          Redirect(amendRoutes.AmendRegistrationController.registrationUpdateFailed())
      }.recover {
        case _: Exception =>
          Redirect(amendRoutes.AmendRegistrationController.registrationUpdateFailed())
      }
    }

  protected def getBackLink(memberId: String): Call =
    routes.AddGroupMemberContactDetailsConfirmAddressController.displayPage(memberId)

  protected def getSubmitCall(memberId: String): Call =
    routes.AddGroupMemberContactDetailsCheckAnswersController.submit()

}