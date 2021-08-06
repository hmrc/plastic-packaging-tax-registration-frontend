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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.helpers.LiabilityLinkHelper
import uk.gov.hmrc.plasticpackagingtax.registration.forms.ExpectToExceedThresholdWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability_expect_to_exceed_threshold_weight_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityExpectToExceedThresholdWeightController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  page: liability_expect_to_exceed_threshold_weight_page,
  liabilityLinkHelper: LiabilityLinkHelper
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.liabilityDetails.expectToExceedThresholdWeight match {
        case Some(data) =>
          Future(
            Ok(
              page(
                ExpectToExceedThresholdWeight.form().fill(ExpectToExceedThresholdWeight(Some(data)))
              )
            )
          )
        case _ => Future(Ok(page(ExpectToExceedThresholdWeight.form())))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      ExpectToExceedThresholdWeight.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[ExpectToExceedThresholdWeight]) =>
            Future(BadRequest(page(formWithErrors))),
          processMoreWeight =>
            updateRegistration(processMoreWeight).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    if (processMoreWeight.answer.getOrElse(false))
                      Redirect(liabilityLinkHelper.nextPage())
                    else Redirect(routes.NotLiableController.displayPage())
                  case _ => Redirect(routes.RegistrationController.displayPage())
                }
              case Left(error) => throw error
            }
        )

    }

  private def updateRegistration(
    formData: ExpectToExceedThresholdWeight
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedLiableDetails =
        registration.liabilityDetails.copy(expectToExceedThresholdWeight = formData.answer)
      registration.copy(liabilityDetails = updatedLiableDetails)
    }

}
