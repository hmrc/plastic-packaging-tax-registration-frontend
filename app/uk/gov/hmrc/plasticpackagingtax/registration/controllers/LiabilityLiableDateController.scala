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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, LiabilityLiableDate}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_liable_date_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityLiableDateController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: liability_liable_date_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.liabilityDetails.isLiable match {
        case Some(data) =>
          Future(Ok(page(LiabilityLiableDate.form().fill(LiabilityLiableDate(Some(data))))))
        case _ => Future(Ok(page(LiabilityLiableDate.form())))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      LiabilityLiableDate.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[LiabilityLiableDate]) => Future(BadRequest(page(formWithErrors))),
          liableDate =>
            updateRegistration(liableDate).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    if (liableDate.answer.getOrElse(false))
                      Redirect(routes.CheckLiabilityDetailsAnswersController.displayPage())
                    else Redirect(routes.NotLiableController.displayPage())
                  case _ => Redirect(routes.RegistrationController.displayPage())
                }
              case Left(error) => throw error
            }
        )

    }

  private def updateRegistration(
    formData: LiabilityLiableDate
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiableDetails =
        registration.liabilityDetails.copy(isLiable = formData.answer,
                                           startDate = startDate(formData)
        )
      registration.copy(liabilityDetails = updatedLiableDetails)
    }

  private def startDate(formData: LiabilityLiableDate): Option[Date] =
    formData.answer match {
      case Some(true)  => Some(Date(Some(1), Some(4), Some(2022)))
      case Some(false) => None
    }

}
