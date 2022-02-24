/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.{
  AddressCaptureConfig,
  AddressCaptureService
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendOrganisationDetailsController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  mcc: MessagesControllerComponents,
  addressCaptureService: AddressCaptureService,
  amendmentJourneyAction: AmendmentJourneyAction
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendmentJourneyAction) {

  def changeBusinessAddress(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      initialiseAddressLookup(request)
    }

  private def initialiseAddressLookup(
    request: JourneyRequest[AnyContent]
  )(implicit header: HeaderCarrier): Future[Result] =
    addressCaptureService.initAddressCapture(
      AddressCaptureConfig(backLink = routes.AmendRegistrationController.displayPage().url,
                           successLink =
                             routes.AmendOrganisationDetailsController.addressCaptureCallback().url,
                           alfHeadingsPrefix = "addressLookup.business",
                           entityName = request.registration.organisationDetails.businessName,
                           pptHeadingKey = "addressCapture.business.heading",
                           pptHintKey = None
      )
    )(request).map(redirect => Redirect(redirect))

  def addressCaptureCallback(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      def updateBusinessAddress(address: Option[Address]): Registration => Registration = {
        registration: Registration =>
          val updatedOrganisationDetails =
            registration.organisationDetails.copy(businessRegisteredAddress =
              address
            )
          registration.copy(organisationDetails = updatedOrganisationDetails)
      }

      addressCaptureService.getCapturedAddress().flatMap {
        capturedAddress =>
          updateRegistration(updateBusinessAddress(capturedAddress)).map { _ =>
            Redirect(routes.AmendRegistrationController.displayPage())
          }
      }
    }

}
