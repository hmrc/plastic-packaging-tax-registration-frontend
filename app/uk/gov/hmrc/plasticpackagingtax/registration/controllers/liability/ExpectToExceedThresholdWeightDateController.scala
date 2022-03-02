/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExpectToExceedThresholdWeightDate
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.expect_to_exceed_threshold_weight_date_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExpectToExceedThresholdWeightDateController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  expectToExceedThresholdWeightDate: ExpectToExceedThresholdWeightDate,
  page: expect_to_exceed_threshold_weight_date_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      val form =
        request.registration.liabilityDetails.dateRealisedExpectedToExceedThresholdWeight match {
          case Some(data) =>
            expectToExceedThresholdWeightDate().fill(data)
          case _ =>
            expectToExceedThresholdWeightDate()
        }
      Ok(page(form))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      expectToExceedThresholdWeightDate()
        .bindFromRequest()
        .fold((formWithErrors: Form[Date]) => Future.successful(BadRequest(page(formWithErrors))),
              dateRealisedExpectedToExceedThresholdWeight =>
                updateRegistration(dateRealisedExpectedToExceedThresholdWeight).map {
                  case Right(_)    => Redirect(routes.LiabilityWeightController.displayPage())
                  case Left(error) => throw error
                }
        )
    }

  private def updateRegistration(
    dateRealisedExpectedToExceedThresholdWeight: Date
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(liabilityDetails =
        registration.liabilityDetails.copy(dateRealisedExpectedToExceedThresholdWeight =
          Some(dateRealisedExpectedToExceedThresholdWeight)
        )
      )
    }

}
