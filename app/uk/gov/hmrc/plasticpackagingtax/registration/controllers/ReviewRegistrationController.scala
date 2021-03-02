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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  IncorpIdConnector,
  RegistrationConnector,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.review_registration_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

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
              _                    <- updateRegistration(hasReviewedRegistration = true)
            } yield Ok(page(request.registration, incorporationDetails))
          case _ => Future(Redirect(routes.RegistrationController.displayPage()))
        }
      else
        Future(Redirect(routes.RegistrationController.displayPage()))
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction) { _ =>
      Redirect(routes.RegistrationController.displayPage())
    }

  private def updateRegistration(
    hasReviewedRegistration: Boolean
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedMetaData =
        registration.metaData.copy(hasReviewedRegistration = hasReviewedRegistration)
      registration.copy(metaData = updatedMetaData)
    }

}
