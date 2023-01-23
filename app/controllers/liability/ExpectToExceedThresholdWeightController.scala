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
import controllers.actions.NotEnrolledAuthAction
import forms.Date
import forms.liability.{ExpectToExceedThresholdWeight, ExpectToExceedThresholdWeightAnswer}
import models.registration.{Cacheable, NewLiability, Registration}
import models.request.{JourneyAction, JourneyRequest}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.liability.expect_to_exceed_threshold_weight_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExpectToExceedThresholdWeightController @Inject() (
                                                          authenticate: NotEnrolledAuthAction,
                                                          journeyAction: JourneyAction,
                                                          override val registrationConnector: RegistrationConnector,
                                                          mcc: MessagesControllerComponents,
                                                          page: expect_to_exceed_threshold_weight_page,
                                                          form: ExpectToExceedThresholdWeight,
                                                          appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      (request.registration.liabilityDetails.expectToExceedThresholdWeight, request.registration.liabilityDetails.dateRealisedExpectedToExceedThresholdWeight) match {
        case (Some(yesNo), date) => Ok(page(form().fill(ExpectToExceedThresholdWeightAnswer(yesNo,date.map(_.date)))))
        case _ => Ok(page(form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
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
    expectToExceedThresholdWeight: ExpectToExceedThresholdWeightAnswer
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiableDetails =
        registration.liabilityDetails.copy(
          expectToExceedThresholdWeight = Some(expectToExceedThresholdWeight.yesNo),
          dateRealisedExpectedToExceedThresholdWeight = expectToExceedThresholdWeight.date.map(Date.apply),
          newLiabilityStarted = Some(NewLiability)
        )
      registration.copy(liabilityDetails = updatedLiableDetails)
    }

  private def exceededThresholdLink: Call = {
    if(appConfig.isBackLookChangeEnabled)
      controllers.liability.routes.ExceededThresholdWeightController.displayPage()
    else controllers.liability.routes.ExceededThresholdWeightController.displayPageBeforeApril2023()
  }

}
