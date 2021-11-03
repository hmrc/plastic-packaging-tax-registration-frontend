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
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  EmailVerificationConnector,
  RegistrationConnector,
  ServiceError
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationStatus.{
  LOCKED_OUT,
  NOT_VERIFIED,
  VERIFIED
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification._
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.email_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsEmailAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  emailVerificationConnector: EmailVerificationConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
  appConfig: AppConfig,
  page: email_address_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.primaryContactDetails.email match {
        case Some(data) =>
          Ok(page(EmailAddress.form().fill(EmailAddress(data))))
        case _ => Ok(page(EmailAddress.form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(BadRequest(page(formWithErrors))),
          emailAddress =>
            updateRegistration(formData = emailAddress, credId = request.user.credId).flatMap {
              case Right(registration) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    saveAndContinue(registration, request.user.credId)
                  case _ =>
                    Future(Redirect(routes.RegistrationController.displayPage()))
                }
              case Left(error) => throw error
            }
        )
    }

  private def updateRegistration(formData: EmailAddress, credId: String)(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    emailVerificationConnector.getStatus(credId).flatMap {
      case Right(emailStatusResponse) =>
        emailStatusResponse match {
          case Some(response) =>
            update { registration =>
              registration.copy(primaryContactDetails =
                                  updatedPrimaryContactDetails(formData, registration),
                                metaData = registration.metaData.add(response.emails)
              )
            }
          case None =>
            update { registration =>
              registration.copy(primaryContactDetails =
                updatedPrimaryContactDetails(formData, registration)
              )
            }
        }
      case Left(error) => throw error
    }

  private def updatedPrimaryContactDetails(formData: EmailAddress, registration: Registration) =
    registration.primaryContactDetails.copy(email = Some(formData.value))

  private def updatedJourneyId(registration: Registration, journeyId: String)(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] = {
    val primaryContact = registration.primaryContactDetails.copy(journeyId = Some(journeyId))
    update {
      registration => registration.copy(primaryContactDetails = primaryContact)
    }
  }

  private def saveAndContinue(registration: Registration, credId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Result] =
    if (appConfig.emailVerificationEnabled)
      EmailVerificationService.getPrimaryEmailStatus(registration) match {
        case VERIFIED =>
          Future(Redirect(routes.ContactDetailsTelephoneNumberController.displayPage()))
        case NOT_VERIFIED =>
          handleNotVerifiedEmail(registration, credId)
        case LOCKED_OUT =>
          Future(Redirect(routes.RegistrationController.displayPage()))
      }
    else
      Future(Redirect(routes.ContactDetailsTelephoneNumberController.displayPage()))

  private def createEmailVerification(credId: String, email: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, String]] =
    emailVerificationConnector.create(
      CreateEmailVerificationRequest(credId = credId,
                                     continueUrl =
                                       "/register-for-plastic-packaging-tax/primary-contact-details",
                                     origin = "ppt",
                                     accessibilityStatementUrl = "/accessibility",
                                     email = Email(address = email, enterUrl = "/start"),
                                     backUrl = "/back",
                                     pageTitle = "PPT Title",
                                     deskproServiceName = "plastic-packaging-tax"
      )
    )

  private def handleNotVerifiedEmail(registration: Registration, credId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Result] =
    registration.primaryContactDetails.email match {
      case Some(emailAddress) =>
        createEmailVerification(credId, emailAddress).flatMap {
          case Right(verificationJourneyStartUrl) =>
            updatedJourneyId(registration,
                             verificationJourneyStartUrl.split("/").slice(0, 4).last
            ).flatMap {
              case Left(error) => throw error
              case Right(registration) =>
                Future(Redirect(routes.ContactDetailsEmailAddressPasscodeController.displayPage()))
            }

          case Left(error) => throw error
        }
      case None => throw RegistrationException("Failed to get email from the cache")
    }

}

case class RegistrationException(message: String) extends Exception
