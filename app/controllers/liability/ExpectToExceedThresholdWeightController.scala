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
import forms.Date
import forms.liability.ExpectToExceedThresholdWeight
import models.registration.{Cacheable, NewLiability, Registration}
import models.request.JourneyRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.liability.expect_to_exceed_threshold_weight_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExpectToExceedThresholdWeightController @Inject() (
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: expect_to_exceed_threshold_weight_page,
  form: ExpectToExceedThresholdWeight
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Cacheable
    with I18nSupport {

  def displayPage(): Action[AnyContent] =
    journeyAction.register { implicit request =>
      request.registration.liabilityDetails.expectToExceedThresholdWeight match {
        case Some(yesNo) => Ok(page(form().fill(yesNo)))
        case _           => Ok(page(form()))
      }
    }

  def submit(): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      form()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(page(formWithErrors))),
          expectToExceed =>
            updateRegistration(expectToExceed).map {
              case Right(_)    => redirect(expectToExceed)
              case Left(error) => throw error
            }
        )

    }

  private def redirect(isYes: Boolean): Result =
    if (isYes)
      Redirect(controllers.liability.routes.ExpectToExceedThresholdWeightDateController.displayPage)
    else
      Redirect(controllers.liability.routes.ExceededThresholdWeightController.displayPage)

  private def updateRegistration(
    expectToExceedThresholdWeight: Boolean
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiableDetails =
        registration.liabilityDetails.copy(
          expectToExceedThresholdWeight = Some(expectToExceedThresholdWeight),
          dateRealisedExpectedToExceedThresholdWeight = resetDateRealised(expectToExceedThresholdWeight, registration),
          newLiabilityStarted = Some(NewLiability)
        )
      registration.copy(liabilityDetails = updatedLiableDetails)
    }

  private def resetDateRealised(expectToExceedThresholdWeight: Boolean, registration: Registration): Option[Date] =
    if (expectToExceedThresholdWeight)
      registration.liabilityDetails.dateRealisedExpectedToExceedThresholdWeight
    else None

}
