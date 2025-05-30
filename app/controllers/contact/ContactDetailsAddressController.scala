/*
 * Copyright 2025 HM Revenue & Customs
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

import connectors.{RegistrationConnector, ServiceError}
import controllers.actions.JourneyAction
import forms.contact.Address
import models.registration.{Cacheable, PrimaryContactDetails, Registration}
import models.request.JourneyRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AddressCaptureConfig, AddressCaptureService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsAddressController @Inject() (
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  addressCaptureService: AddressCaptureService,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Cacheable
    with I18nSupport {

  def displayPage: Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      addressCaptureService.initAddressCapture(
        AddressCaptureConfig(
          backLink = routes.ContactDetailsConfirmAddressController.displayPage().url,
          successLink = routes.ContactDetailsAddressController.update.url,
          alfHeadingsPrefix = "addressLookup.contact",
          entityName = request.registration.organisationDetails.businessName,
          pptHeadingKey = "addressCapture.contact.heading",
          pptHintKey = None,
          forceUkAddress = false
        )
      )(request.authenticatedRequest).map(redirect => Redirect(redirect))

    }

  def update: Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      addressCaptureService.getCapturedAddress()(request.authenticatedRequest).flatMap { capturedAddress =>
        updateRegistration(capturedAddress).map {
          case Right(_) =>
            Redirect(routes.ContactDetailsCheckAnswersController.displayPage())
          case Left(error) => throw error
        }
      }
    }

  private def updateRegistration(
    formData: Option[Address]
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(primaryContactDetails = updateAddress(formData, registration))
    }

  private def updateAddress(address: Option[Address], registration: Registration): PrimaryContactDetails =
    registration.primaryContactDetails.copy(address = address)

}
