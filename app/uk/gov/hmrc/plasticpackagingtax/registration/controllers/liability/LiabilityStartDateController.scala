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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{AuthAction, FormAction, SaveAndContinue}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, OldDate}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityStartDate
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.liability_start_date_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityStartDateController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: liability_start_date_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.liabilityDetails.startDate match {
        case Some(data) =>
          Ok(page(LiabilityStartDate.form().fill(data), backLink))
        case _ =>
          Ok(page(LiabilityStartDate.form(), backLink))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      LiabilityStartDate.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[OldDate]) =>
            Future.successful(BadRequest(page(formWithErrors, backLink))),
          liabilityStartDate =>
            updateRegistration(liabilityStartDate).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    Redirect(routes.RegistrationTypeController.displayPage())

                  case _ =>
                    Redirect(commonRoutes.TaskListController.displayPage())
                }
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: OldDate
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { model =>
      val updatedLiabilityDetails = model.liabilityDetails.copy(startDate = Some(formData),
                                                                weight =
                                                                  model.liabilityDetails.weight
      )
      model.copy(liabilityDetails = updatedLiabilityDetails)
    }

  private def backLink()(implicit request: JourneyRequest[AnyContent]) =
    request.registration.liabilityDetails.weight match {
      case Some(weight) =>
        weight.totalKg match {
          case Some(totalKg) =>
            if (totalKg < deMinimisKg)
              routes.LiabilityExpectToExceedThresholdWeightController.displayPage()
            else routes.LiabilityWeightController.displayPage()
          case None => commonRoutes.TaskListController.displayPage()
        }
      case None => commonRoutes.TaskListController.displayPage()
    }

}
