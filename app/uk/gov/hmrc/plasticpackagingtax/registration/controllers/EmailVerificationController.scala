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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  DownstreamServiceError,
  EmailVerificationConnector,
  RegistrationConnector
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationStatus.{
  LOCKED_OUT,
  NOT_VERIFIED,
  VERIFIED
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{
  EmailStatus,
  EmailVerificationService
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class EmailVerificationController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  emailVerificationConnector: EmailVerificationConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def emailVerificationCallback(credId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async {
      implicit request =>
        emailVerificationConnector.getStatus(credId).flatMap {
          case Right(emailStatusResponse) =>
            emailStatusResponse match {
              case Some(response) => handleResponse(response.emails)
              case None =>
                throw DownstreamServiceError(
                  "Error while getting email verification status. No data returned for user."
                )
            }
          case Left(error) => throw error
        }
    }

  private def handleResponse(
    emailStatuses: Seq[EmailStatus]
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]) =
    updateMetadata(emailStatuses).map {
      case Right(registration) =>
        EmailVerificationService.getPrimaryEmailStatus(registration) match {
          case VERIFIED =>
            Redirect(routes.ContactDetailsTelephoneNumberController.displayPage())
          case LOCKED_OUT =>
            Redirect(routes.RegistrationController.displayPage())
          case NOT_VERIFIED =>
            Redirect(routes.ContactDetailsEmailAddressController.displayPage())
        }
      case Left(error) => throw error
    }

  private def updateMetadata(
    emails: Seq[EmailStatus]
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]) =
    update { registration =>
      registration.copy(metaData = registration.metaData.add(emails))
    }

}
