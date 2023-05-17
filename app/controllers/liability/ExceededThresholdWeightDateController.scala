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
import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import forms.Date
import forms.liability.ExceededThresholdWeightDate
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import play.api.i18n.I18nSupport
import play.api.mvc._
import views.html.liability.exceeded_threshold_weight_date_page

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExceededThresholdWeightDateController @Inject()(
                                                    journeyAction: JourneyAction,
                                                    appConfig: AppConfig,
                                                    override val registrationConnector: RegistrationConnector,
                                                    mcc: MessagesControllerComponents,
                                                    form: ExceededThresholdWeightDate,
                                                    page: exceeded_threshold_weight_date_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>

      val filledForm = request.registration.liabilityDetails.dateExceededThresholdWeight match {
        case Some(date) => form().fill(date.date)
        case _          => form()
      }

      Ok(page(filledForm))
    }

  def submit(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      form().bindFromRequest().fold(
        errorForm =>
          Future.successful(BadRequest(page(errorForm)))
        , alreadyExceeded =>
          updateRegistration(alreadyExceeded)
            .map {
              case Right(_) => Redirect(routes.TaxStartDateController.displayPage())
              case Left(error) => throw error
            }
      )
    }

  private def updateRegistration(
                                  exceededDate: LocalDate
                                )(implicit request: JourneyRequest[_]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(liabilityDetails =
        registration.liabilityDetails.copy(
          dateExceededThresholdWeight = Some(Date(exceededDate))
      ))
    }

}
