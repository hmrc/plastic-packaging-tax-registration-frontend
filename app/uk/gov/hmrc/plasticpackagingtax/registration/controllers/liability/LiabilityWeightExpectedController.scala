/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityExpectedWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.liability_weight_expected_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityWeightExpectedController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: liability_weight_expected_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.liabilityDetails.expectedWeight match {
        case Some(data) => Ok(page(LiabilityExpectedWeight.form().fill(data)))
        case _          => Ok(page(LiabilityExpectedWeight.form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      LiabilityExpectedWeight.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[LiabilityExpectedWeight]) =>
            Future.successful(BadRequest(page(formWithErrors))),
          liabilityWeight =>
            updateRegistration(liabilityWeight).map {
              case Right(_)    => nextPage(liabilityWeight)
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: LiabilityExpectedWeight
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val startDate =
        if (formData.overLiabilityThreshold)
          Some(Date(Some(1), Some(4), Some(2022)))
        else None
      val updatedLiabilityDetails =
        registration.liabilityDetails.copy(startDate = startDate,
                                           expectedWeight = Some(formData),
                                           isLiable = Some(formData.overLiabilityThreshold)
        )
      registration.copy(liabilityDetails = updatedLiabilityDetails)
    }

  private def nextPage(
    formData: LiabilityExpectedWeight
  )(implicit req: JourneyRequest[AnyContent]): Result =
    if (formData.overLiabilityThreshold)
      if (isGroupRegistrationEnabled)
        Redirect(routes.RegistrationTypeController.displayPage())
      else
        Redirect(commonRoutes.TaskListController.displayPage())
    else
      Redirect(routes.NotLiableController.displayPage())

}
