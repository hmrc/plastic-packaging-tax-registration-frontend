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

import config.AppConfig
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.NotEnrolledAuthAction
import forms.liability.{ExceededThresholdWeight, ExceededThresholdWeightAnswer}
import models.registration.{Cacheable, Registration}
import models.request.{JourneyAction, JourneyRequest}
import views.html.liability.exceeded_threshold_weight_page
import forms.Date

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExceededThresholdWeightController @Inject() (
                                                    authenticate: NotEnrolledAuthAction,
                                                    journeyAction: JourneyAction,
                                                    appConfig: AppConfig,
                                                    override val registrationConnector: RegistrationConnector,
                                                    mcc: MessagesControllerComponents,
                                                    exceededThresholdWeight: ExceededThresholdWeight,
                                                    page: exceeded_threshold_weight_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>

      val backLookChangeEnabled = appConfig.backLookChangeEnabled
      (request.registration.liabilityDetails.exceededThresholdWeight, request.registration.liabilityDetails.dateExceededThresholdWeight)  match {
        case (Some(yesNo), date) => Ok(page(
          exceededThresholdWeight.form().fill(ExceededThresholdWeightAnswer(yesNo, date.map(_.date))), backLookChangeEnabled))
        case _          => Ok(page(exceededThresholdWeight.form(), backLookChangeEnabled))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      exceededThresholdWeight.form().bindFromRequest().fold(hasErrors, onSuccess)
    }

  private def hasErrors(form: Form[_])(implicit request: Request[_]): Future[Result] =
    Future.successful(BadRequest(page(form, appConfig.backLookChangeEnabled)))

  private def updateRegistration(
    alreadyExceeded: ExceededThresholdWeightAnswer
  )(implicit request: JourneyRequest[_]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiabilityDetails = {
        registration.liabilityDetails.copy(
          exceededThresholdWeight = Some(alreadyExceeded.yesNo),
          dateExceededThresholdWeight = alreadyExceeded.date.map(Date.apply)
        )
      }
      registration.copy(liabilityDetails = updatedLiabilityDetails)
    }

  private def onSuccess(
    alreadyExceeded: ExceededThresholdWeightAnswer
  )(implicit request: JourneyRequest[_]): Future[Result] = {
    val future = updateRegistration(alreadyExceeded)
    future
      .map({
        case Left(error) => throw error
        case _           => Redirect(routes.TaxStartDateController.displayPage())
      })
  }

}
