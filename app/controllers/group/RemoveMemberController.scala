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

import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import forms.group.RemoveMember
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.group.remove_group_member_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveMemberController @Inject() (
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: remove_group_member_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Cacheable
    with I18nSupport
    with RemoveMemberAction
    with Logging {

  def displayPage(groupMemberId: String): Action[AnyContent] =
    journeyAction.register { implicit request: JourneyRequest[AnyContent] =>
      getGroupMemberName(groupMemberId) match {
        case Some(groupMemberName) => Ok(page(RemoveMember.form(), groupMemberName, groupMemberId))
        case _ =>
          Redirect(routes.OrganisationListController.displayPage())
      }

    }

  def submit(groupMemberId: String): Action[AnyContent] =
    journeyAction.register.async { implicit request: JourneyRequest[AnyContent] =>
      getGroupMemberName(groupMemberId) match {
        case Some(groupMemberName) =>
          RemoveMember.form()
            .bindFromRequest()
            .fold(
              (formWithErrors: Form[RemoveMember]) =>
                Future.successful(BadRequest(page(formWithErrors, groupMemberName, groupMemberId))),
              removeMember =>
                removeMember.value match {
                  case Some(true) =>
                    removeGroupMember(groupMemberId).map {
                      case Right(_) =>
                        Redirect(routes.OrganisationListController.displayPage())
                      case Left(error) =>
                        logger.warn(
                          s"Failed to remove group member [$groupMemberName] with id [$groupMemberId] - ${error.getMessage}",
                          error
                        )
                        Redirect(routes.OrganisationListController.displayPage())
                    }
                  case _ =>
                    Future.successful(Redirect(routes.OrganisationListController.displayPage()))
                }
            )
        case _ => Future.successful(Redirect(routes.OrganisationListController.displayPage()))
      }
    }

  private def getGroupMemberName(groupMemberId: String)(implicit request: JourneyRequest[AnyContent]): Option[String] =
    request.registration.groupDetail
      .flatMap(_.members.find(_.id == groupMemberId))
      .flatMap(_.organisationDetails.map(_.organisationName))

  private def removeGroupMember(
    groupMemberId: String
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      doRemoveMemberAction(registration, groupMemberId)
    }

}
