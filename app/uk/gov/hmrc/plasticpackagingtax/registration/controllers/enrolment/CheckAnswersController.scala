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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment

import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.enrolment.UserEnrolmentConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.{
  EnrolmentFailureCode,
  UserEnrolmentFailedResponse,
  UserEnrolmentSuccessResponse
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.UserEnrolmentDetails
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserEnrolmentDetailsRepository
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.check_answers_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckAnswersController @Inject() (
  authenticate: AuthAction,
  mcc: MessagesControllerComponents,
  cache: UserEnrolmentDetailsRepository,
  userEnrolmentConnector: UserEnrolmentConnector,
  page: check_answers_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    authenticate.async { implicit request =>
      cache.get().map {
        case answers if answers.isComplete => Ok(page(answers))
        case _                             => Redirect(routes.PptReferenceController.displayPage())
      }
    }

  def submit(): Action[AnyContent] =
    authenticate.async { implicit request =>
      for {
        answers <- cache.get()
        result  <- handleEnrolment(answers)
        _       <- cache.delete()
      } yield result
    }

  private def handleEnrolment(
    userEnrolmentDetails: UserEnrolmentDetails
  )(implicit hc: HeaderCarrier): Future[Result] =
    userEnrolmentConnector.enrol(userEnrolmentDetails).map {
      case response @ UserEnrolmentSuccessResponse(_) =>
        Redirect(routes.ConfirmationController.displayPage())
      case UserEnrolmentFailedResponse(_, failureCode)
          if failureCode == EnrolmentFailureCode.VerificationFailed || failureCode == EnrolmentFailureCode.VerificationMissing =>
        Redirect(routes.NotableErrorController.enrolmentVerificationFailurePage())
    }

}
