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
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.AddressLookupIntegration
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.{
  AddressLookupConfirmation,
  MissingAddressIdException
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  JourneyRequest
}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendOrganisationDetailsController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  mcc: MessagesControllerComponents,
  addressLookupFrontendConnector: AddressLookupFrontendConnector,
  appConfig: AppConfig,
  amendmentJourneyAction: AmendmentJourneyAction
)(implicit ec: ExecutionContext)
    extends AmendmentController(mcc, amendmentJourneyAction) with AddressLookupIntegration {

  def changeBusinessAddress(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      initialiseAddressLookup(request)
    }

  private def initialiseAddressLookup(
    request: JourneyRequest[AnyContent]
  )(implicit header: HeaderCarrier): Future[Result] =
    initialiseAddressLookup(addressLookupFrontendConnector,
                            appConfig,
                            routes.AmendOrganisationDetailsController.alfCallback(None),
                            "addressLookup.business",
                            request.registration.organisationDetails.businessName
    ).map(onRamp => Redirect(onRamp.redirectUrl))

  def alfCallback(id: Option[String]): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction).async { implicit request =>
      addressLookupFrontendConnector.getAddress(
        id.getOrElse(throw new MissingAddressIdException)
      ).flatMap {
        confirmedAddress =>
          updateRegistration(updateBusinessAddress(confirmedAddress)).map { _ =>
            Redirect(routes.AmendRegistrationController.displayPage())
          }
      }
    }

  private def updateBusinessAddress(
    address: AddressLookupConfirmation
  ): Registration => Registration = {
    registration: Registration =>
      registration.copy(organisationDetails =
        registration.organisationDetails.copy(businessRegisteredAddress =
          Some(Address(address))
        )
      )
  }

}
