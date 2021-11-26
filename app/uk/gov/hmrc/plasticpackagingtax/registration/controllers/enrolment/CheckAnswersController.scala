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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.enrolment.UserEnrolmentConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.{
  EnrolmentFailureCode,
  UserEnrolmentFailedResponse,
  UserEnrolmentSuccessResponse
}
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserEnrolmentDetailsRepository
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.check_answers_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

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
        result <-
          userEnrolmentConnector.enrol(answers).map {
            case _ @UserEnrolmentSuccessResponse(_) =>
              cache.delete()
              Redirect(routes.ConfirmationController.displayPage())
            case UserEnrolmentFailedResponse(_, failureCode) =>
              failureCode match {
                case EnrolmentFailureCode.VerificationFailed |
                    EnrolmentFailureCode.VerificationMissing =>
                  Redirect(routes.NotableErrorController.enrolmentVerificationFailurePage())
                case EnrolmentFailureCode.GroupEnrolled =>
                  Redirect(routes.NotableErrorController.enrolmentReferenceNumberAlreadyUsedPage())
                case code => throw new Exception(s"Enrolment failed with code $code")
              }
          }
      } yield result
    }

}
