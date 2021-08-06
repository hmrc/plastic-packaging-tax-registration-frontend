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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.helpers.LiabilityLinkHelper
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_weight_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityWeightController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: liability_weight_page,
  liabilityLinkHelper: LiabilityLinkHelper
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.liabilityDetails.weight match {
        case Some(data) => Ok(page(LiabilityWeight.form().fill(data)))
        case _          => Ok(page(LiabilityWeight.form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      LiabilityWeight.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[LiabilityWeight]) =>
            Future.successful(BadRequest(page(formWithErrors))),
          liabilityWeight =>
            updateRegistration(liabilityWeight).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    handleSaveAndContinue(liabilityWeight)
                  case _ =>
                    Redirect(routes.RegistrationController.displayPage())
                }
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(
    formData: LiabilityWeight
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiabilityDetails =
        registration.liabilityDetails.copy(startDate = registration.liabilityDetails.startDate,
                                           weight = Some(formData)
        )
      registration.copy(liabilityDetails = updatedLiabilityDetails)
    }

  private def handleSaveAndContinue(formData: LiabilityWeight): Result =
    formData.totalKg match {
      case Some(weight) =>
        if (weight <= 10000)
          Redirect(routes.LiabilityExpectToExceedThresholdWeightController.displayPage())
        else Redirect(liabilityLinkHelper.nextPage)
      case None => BadRequest
    }

}
