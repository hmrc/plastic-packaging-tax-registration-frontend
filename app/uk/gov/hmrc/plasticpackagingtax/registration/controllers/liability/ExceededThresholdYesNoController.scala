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
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  DownstreamServiceError,
  RegistrationConnector,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.ExceededThresholdYesNo
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.exceeded_threshold_yes_no_page
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExceededThresholdYesNoController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: exceeded_threshold_yes_no_page
)(implicit ec: ExecutionContext)
    extends LiabilityController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.liabilityDetails.exceededThresholdWeight match {
        case Some(data) => Ok(page(ExceededThresholdYesNo.form().fill(data)))
        case _          => Ok(page(ExceededThresholdYesNo.form()))
      }
    }

  def hasErrors(form: Form[Boolean])(implicit request: Request[_]): Future[Result] =
    Future.successful(BadRequest(page(form)))

  def updateRegistration(
    alreadyExceeded: Boolean
  )(implicit request: JourneyRequest[_]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiabilityDetails =
        registration.liabilityDetails.copy(exceededThresholdWeight = Some(alreadyExceeded))
      registration.copy(liabilityDetails = updatedLiabilityDetails)
    }

  def nextPage(alreadyExceeded: Boolean): Call =
    if (alreadyExceeded)
      // todo should be page where we ask for date user exceeded
      routes.LiabilityStartDateController.displayPage()
    else
      // Haven't already exceeded so ask if they will exceed next
      routes.ExpectToExceedThresholdWeightController.displayPage()

  def onSuccess(alreadyExceeded: Boolean)(implicit request: JourneyRequest[_]): Future[Result] = {
    val future = updateRegistration(alreadyExceeded)
    future
      .map({
        case Left(error) => throw error
        case _           => Redirect(nextPage(alreadyExceeded))
      })
  }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      ExceededThresholdYesNo.form().bindFromRequest().fold(hasErrors, onSuccess)
    }

}
