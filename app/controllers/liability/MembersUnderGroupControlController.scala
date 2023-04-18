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

package controllers.liability

import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import forms.liability.MembersUnderGroupControl
import models.registration.{Cacheable, GroupDetail, Registration}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.liability.members_under_group_control_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MembersUnderGroupControlController @Inject() (
                                                     journeyAction: JourneyAction,
                                                     mcc: MessagesControllerComponents,
                                                     page: members_under_group_control_page,
                                                     override val registrationConnector: RegistrationConnector
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.groupDetail match {
        case Some(groupDetail) =>
          Ok(
            page(
              MembersUnderGroupControl.form().fill(
                MembersUnderGroupControl(
                  Some(groupDetail.membersUnderGroupControl.getOrElse(false))
                )
              )
            )
          )
        case _ => Ok(page(MembersUnderGroupControl.form()))
      }
    }

  def submit(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      MembersUnderGroupControl.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[MembersUnderGroupControl]) =>
            Future.successful(BadRequest(page(formWithErrors))),
          membersUnderGroupControl =>
            updateRegistration(membersUnderGroupControl).map {
              case Right(_)    => nextPage(membersUnderGroupControl)
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: MembersUnderGroupControl
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedGroupDetail =
        registration.groupDetail.getOrElse(GroupDetail()).copy(membersUnderGroupControl =
          formData.value
        )
      registration.copy(groupDetail = Some(updatedGroupDetail))
    }

  private def nextPage(formData: MembersUnderGroupControl): Result =
    if (formData.value.getOrElse(false))
      Redirect(routes.CheckLiabilityDetailsAnswersController.displayPage())
    else
      Redirect(routes.NotMembersUnderGroupControlController.displayPage())

}
