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

import config.AppConfig
import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import forms.Date
import forms.liability.ExpectToExceedThresholdWeightDate
import models.registration.{Cacheable, Registration}
import models.request.JourneyRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.liability.expect_to_exceed_threshold_weight_date_page

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExpectToExceedThresholdWeightDateController@Inject() (
                                                             journeyAction: JourneyAction,
                                                             override val registrationConnector: RegistrationConnector,
                                                             mcc: MessagesControllerComponents,
                                                             page: expect_to_exceed_threshold_weight_date_page,
                                                             form: ExpectToExceedThresholdWeightDate,
                                                             appConfig: AppConfig
                                                           )(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage: Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.liabilityDetails.dateRealisedExpectedToExceedThresholdWeight match {
        case Some(date) => Ok(page(form().fill(date.date)))
        case _ => Ok(page(form()))
      }
    }

  def submit: Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      form()
        .bindFromRequest()
        .fold(formWithErrors =>
          Future.successful(BadRequest(page(formWithErrors))),
          expectToExceed =>
            updateRegistration(expectToExceed).map {
              case Right(_) => Redirect(exceededThresholdLink)
              case Left(error) => throw error
            }
        )

    }

  private def updateRegistration(
    dateRealisedExpectedToExceedThresholdWeight: LocalDate
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiableDetails =
        registration.liabilityDetails.copy(
          dateRealisedExpectedToExceedThresholdWeight = Some(Date(dateRealisedExpectedToExceedThresholdWeight))
        )
      registration.copy(liabilityDetails = updatedLiableDetails)
    }

  private def exceededThresholdLink: Call = controllers.liability.routes.ExceededThresholdWeightController.displayPage

}
