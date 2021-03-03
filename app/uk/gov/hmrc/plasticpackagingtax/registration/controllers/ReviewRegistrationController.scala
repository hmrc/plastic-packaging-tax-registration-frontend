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

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Flash, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  IncorpIdConnector,
  RegistrationConnector,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.response.FlashKeys
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class ReviewRegistrationController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  incorpIdConnector: IncorpIdConnector,
  override val registrationConnector: RegistrationConnector,
  page: review_registration_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      if (request.registration.isCheckAndSubmitReady)
        request.registration.incorpJourneyId match {
          case Some(journeyId) =>
            for {
              incorporationDetails <- incorpIdConnector.getDetails(journeyId)
              _                    <- markRegistrationAsReviewed()
            } yield Ok(page(request.registration, incorporationDetails))
          case _ => Future(Redirect(routes.RegistrationController.displayPage()))
        }
      else
        Future(Redirect(routes.RegistrationController.displayPage()))
    }

  private def markRegistrationAsReviewed()(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedMetaData =
        registration.metaData.copy(registrationReviewed = true)
      registration.copy(metaData = updatedMetaData)
    }

  /*
  TODO: This will need to be refactored once:
  1. The PPT EIS/IF Stub has been implemented
  2. The EIS/IF and ETMP 'Create Registration/Subscription' request/response schemas are made available to us
  3. If a submission has been successful we will need to remove the registration/submission document.
  4. Once we have a lot more clarity. KISS.
   */
  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val referenceId = s"PPT12345678${Random.nextInt(1000000)}"
      markRegistrationAsCompleted().map {
        case Right(_) =>
          Redirect(routes.ConfirmationController.displayPage())
            .flashing(Flash(Map(FlashKeys.referenceId -> referenceId)))
        case Left(error) => throw error
      }
    }

  private def markRegistrationAsCompleted()(implicit
    req: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedMetaData =
        registration.metaData.copy(registrationCompleted = true)
      registration.copy(metaData = updatedMetaData)
    }

}
