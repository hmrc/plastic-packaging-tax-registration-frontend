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

package controllers.amendment.group

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import controllers.actions.JourneyAction
import controllers.amendment.AmendmentController
import controllers.group.RemoveMemberAction
import forms.group.RemoveMember
import models.registration.Registration
import models.request.JourneyRequest
import services.AmendRegistrationService
import views.html.amendment.group.confirm_remove_member_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmRemoveMemberController @Inject() (
  journeyAction: JourneyAction,
  amendRegistrationService: AmendRegistrationService,
  mcc: MessagesControllerComponents,
  page: confirm_remove_member_page
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendRegistrationService)
    with I18nSupport
    with RemoveMemberAction {

  private def onwardCall = routes.GroupMembersListController.displayPage()

  def displayPage(memberId: String): Action[AnyContent] =
    journeyAction.amend { implicit request =>
      request.registration.findMember(memberId).map { member =>
        Ok(page(RemoveMember.form(), member))
      }.getOrElse {
        throw new IllegalStateException("Could not find member")
      }
    }

  def submit(memberId: String): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      request.registration.findMember(memberId).map { member =>
        RemoveMember.form().bindFromRequest().fold(
          { formWithErrors: Form[RemoveMember] =>
            Future.successful(BadRequest(page(formWithErrors, member)))
          },
          { removeMember: RemoveMember =>
            removeMember.value match {
              case Some(true) =>
                removeGroupMember(member.id)
              case _ =>
                Future.successful(Redirect(onwardCall))
            }
          }
        )
      }.getOrElse {
        throw new IllegalStateException("Could not find member")
      }
    }

  private def removeGroupMember(groupMemberId: String)(implicit req: JourneyRequest[AnyContent]): Future[Result] = {
    def doAction(registration: Registration): Registration =
      doRemoveMemberAction(registration, groupMemberId)
    updateRegistration(doAction, onwardCall)
  }

}
