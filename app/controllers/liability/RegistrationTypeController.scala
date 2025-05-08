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

package controllers.liability

import connectors.RegistrationConnector
import controllers.actions.JourneyAction
import forms.liability.{RegType, RegistrationType}
import models.registration.{Cacheable, Registration}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import views.html.liability.registration_type_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationTypeController @Inject() (
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: registration_type_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc)
    with Cacheable
    with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.registrationType match {
        case Some(regType) =>
          Ok(page(RegistrationType.form().fill(RegistrationType(Some(regType))), backLink))
        case _ => Ok(page(RegistrationType.form(), backLink))
      }
    }

  def submit(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      RegistrationType.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[RegistrationType]) => Future.successful(BadRequest(page(formWithErrors, backLink))),
          registrationType =>
            update(updateRegistration(registrationType)).map {
              case Right(registration) =>
                registration.registrationType match {
                  case Some(regType) if regType == RegType.GROUP =>
                    Redirect(routes.MembersUnderGroupControlController.displayPage().url)
                  case _ =>
                    Redirect(routes.CheckLiabilityDetailsAnswersController.displayPage().url)
                }
              case Left(error) => throw error
            }
        )
    }

  private def backLink: Call =
    routes.LiabilityWeightController.displayPage()

  private def updateRegistration(registrationType: RegistrationType): Registration => Registration =
    registrationType.value match {
      case Some(RegType.GROUP) =>
        registration => registration.copy(registrationType = registrationType.value)
      case Some(RegType.SINGLE_ENTITY) =>
        registration =>
          registration.copy(
            registrationType = registrationType.value,
            groupDetail = registration.groupDetail.map(_.copy(membersUnderGroupControl = None))
          )
      case other => throw new IllegalStateException(s"Invalid registration type: $other")
    }

}
