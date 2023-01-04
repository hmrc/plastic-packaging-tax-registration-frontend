/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.contact

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.{
  NotEnrolledAuthAction,
  FormAction,
  SaveAndContinue
}
import controllers.{routes => commonRoutes}
import forms.contact.EmailAddress
import models.emailverification.EmailVerificationStatus.{
  LOCKED_OUT,
  NOT_VERIFIED,
  VERIFIED
}
import models.emailverification._
import models.registration.{Cacheable, Registration}
import models.request.{JourneyAction, JourneyRequest}
import services.EmailVerificationService
import views.html.contact.email_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsEmailAddressController @Inject() (
                                                       authenticate: NotEnrolledAuthAction,
                                                       journeyAction: JourneyAction,
                                                       override val registrationConnector: RegistrationConnector,
                                                       emailVerificationService: EmailVerificationService,
                                                       mcc: MessagesControllerComponents,
                                                       page: email_address_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction) { implicit request =>
      request.registration.primaryContactDetails.email match {
        case Some(data) =>
          Ok(buildEmailPage(EmailAddress.form().fill(EmailAddress(data))))
        case _ =>
          Ok(buildEmailPage(EmailAddress.form()))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      EmailAddress.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[EmailAddress]) =>
            Future.successful(BadRequest(buildEmailPage(formWithErrors))),
          emailAddress =>
            updateRegistration(formData = emailAddress, credId = request.user.credId).flatMap {
              case Right(registration) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    saveAndContinue(registration, request.user.credId)
                  case _ =>
                    Future(Redirect(commonRoutes.TaskListController.displayPage()))
                }
              case Left(error) => throw error
            }
        )
    }

  private def buildEmailPage(
    form: Form[EmailAddress]
  )(implicit request: JourneyRequest[AnyContent]) =
    page(
      form,
      routes.ContactDetailsEmailAddressController.submit(),
      request.registration.isGroup
    )

  private def updateRegistration(formData: EmailAddress, credId: String)(implicit
    request: JourneyRequest[AnyContent]
  ): Future[Either[ServiceError, Registration]] =
    emailVerificationService.getStatus(credId).flatMap {
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
    LocalEmailVerification.getPrimaryEmailStatus(registration) match {
      case VERIFIED =>
        Future(Redirect(routes.ContactDetailsTelephoneNumberController.displayPage()))
      case NOT_VERIFIED =>
        handleNotVerifiedEmail(registration, credId)
      case LOCKED_OUT =>
        Future(Redirect(commonRoutes.TaskListController.displayPage()))
    }

  private def createEmailVerification(credId: String, email: String)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    this.emailVerificationService.sendVerificationCode(
      email,
      credId,
      routes.ContactDetailsEmailAddressController.displayPage().url
    )

  private def handleNotVerifiedEmail(registration: Registration, credId: String)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[AnyContent]
  ): Future[Result] =
    registration.primaryContactDetails.email match {
      case Some(emailAddress) =>
        createEmailVerification(credId, emailAddress).flatMap { journeyId =>
          updatedJourneyId(registration, journeyId).map {
            case Left(error) => throw error
            case Right(_) =>
              Redirect(routes.ContactDetailsEmailAddressPasscodeController.displayPage())
          }
        }
      case None => throw RegistrationException("Failed to get email from the cache")
    }

}

case class RegistrationException(message: String) extends Exception
