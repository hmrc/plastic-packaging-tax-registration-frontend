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
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, OptionalRetrieval, Retrieval}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{EmailVerificationConnector, RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{AuthAction, FormAction, SaveAndContinue}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationStatus.{LOCKED_OUT, NOT_VERIFIED, VERIFIED}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.{CreateEmailVerificationRequest, Email}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{Cacheable, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.email_address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsEmailAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  emailVerificationConnector: EmailVerificationConnector,
  override val registrationConnector: RegistrationConnector,
  mcc: MessagesControllerComponents,
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
        .fold((formWithErrors: Form[EmailAddress]) => {
                println(formWithErrors.errors)
                Future.successful(BadRequest(page(formWithErrors)))
              },
              emailAddress =>
                updateRegistration(emailAddress).flatMap {
                  case Right(registration) =>
                    FormAction.bindFromRequest match {
                      case SaveAndContinue =>
                        saveAndContinue(registration)
                      case _ =>
                        Future(Redirect(routes.RegistrationController.displayPage()))
                    }
                  case Left(error) => throw error
                }
        )
    }

  private def updateRegistration(
    formData: EmailAddress
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      val updatedEmailAddress =
        registration.primaryContactDetails.copy(email = Some(formData.value))
      registration.copy(primaryContactDetails = updatedEmailAddress)
    }

  private def saveAndContinue(
    registration: Registration
  )(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Future[Result] =
    registration.getPrimaryContactEmailStatus match {
      case VERIFIED =>
        Future(Redirect(routes.ContactDetailsTelephoneNumberController.displayPage()))
      case NOT_VERIFIED =>
        request.user.identityData.credentials.map { creds =>
          createEmailVerification(creds.providerId).flatMap {
            case Right(verificationJourneyStartUrl) =>
              Future(Redirect(verificationJourneyStartUrl).addingToSession())
            case Left(error) => throw error
          }
        }.getOrElse(Results.Redirect(routes.UnauthorisedController.onPageLoad()))
      case LOCKED_OUT =>
        Future(Redirect(routes.RegistrationController.displayPage()))
    }

  private def createEmailVerification(
    credId: String
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, String]] =
    emailVerificationConnector.create(
      CreateEmailVerificationRequest(credId = credId,
                                     continueUrl = "http://continue",
                                     origin = "origin",
                                     accessibilityStatementUrl = "http://accessibility",
                                     email = Email(address = "test@hmrc.com",
                                                   enterUrl = "hhtp://enterUrl"
                                     ),
                                     backUrl = "http://back",
                                     pageTitle = "PPT Title",
                                     deskproServiceName = "ppt"
      )
    )

}
