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

package controllers.liability

import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import forms.liability.LiabilityWeight
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.liability.liability_weight_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityWeightController @Inject() (
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: liability_weight_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc)
    with Cacheable
    with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.liabilityDetails.expectedWeightNext12m match {
        case Some(data) => Ok(page(LiabilityWeight.form().fill(data)))
        case _          => Ok(page(LiabilityWeight.form()))
      }
    }

  def submit(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      LiabilityWeight.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[LiabilityWeight]) => Future.successful(BadRequest(page(formWithErrors))),
          liabilityWeight =>
            updateRegistration(liabilityWeight).map {
              case Right(_)    => Redirect(routes.RegistrationTypeController.displayPage())
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: LiabilityWeight
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(liabilityDetails = registration.liabilityDetails.copy(expectedWeightNext12m = Some(formData)))
    }

}
