/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.amendment

import controllers.actions.JourneyAction
import forms.contact.Address
import models.registration.Registration
import models.request.JourneyRequest
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AddressCaptureConfig, AddressCaptureService, AmendRegistrationService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendOrganisationDetailsController @Inject() (
  journeyAction: JourneyAction,
  amendRegistrationService: AmendRegistrationService,
  mcc: MessagesControllerComponents,
  addressCaptureService: AddressCaptureService
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendRegistrationService) {

  def changeBusinessAddress(): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      initialiseAddressLookup(request)
    }

  private def initialiseAddressLookup(request: JourneyRequest[AnyContent]): Future[Result] =
    addressCaptureService.initAddressCapture(
      AddressCaptureConfig(
        backLink = routes.AmendRegistrationController.displayPage().url,
        successLink =
          routes.AmendOrganisationDetailsController.addressCaptureCallback().url,
        alfHeadingsPrefix = "addressLookup.business",
        entityName = request.registration.organisationDetails.businessName,
        pptHeadingKey = "addressCapture.business.heading",
        pptHintKey = None,
        forceUkAddress = false
      )
    )(request.authenticatedRequest).map(redirect => Redirect(redirect))

  def addressCaptureCallback(): Action[AnyContent] =
    journeyAction.amend.async { implicit request =>
      def updateBusinessAddress(address: Option[Address]): Registration => Registration = {
        registration: Registration =>
          val updatedOrganisationDetails =
            registration.organisationDetails.copy(businessRegisteredAddress =
              address
            )
          registration.copy(organisationDetails = updatedOrganisationDetails)
      }

      addressCaptureService.getCapturedAddress()(request.authenticatedRequest).flatMap {
        capturedAddress =>
          updateRegistration(updateBusinessAddress(capturedAddress)).map { _ =>
            Redirect(routes.AmendRegistrationController.displayPage())
          }
      }
    }

}
