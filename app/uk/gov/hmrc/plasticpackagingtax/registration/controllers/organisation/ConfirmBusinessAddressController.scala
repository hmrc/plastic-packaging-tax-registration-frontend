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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{
  AddressLookupIntegration,
  routes => commonRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.MissingAddressIdException
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmBusinessAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  addressLookupFrontendConnector: AddressLookupFrontendConnector,
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  page: confirm_business_address
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with AddressLookupIntegration with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.organisationDetails.businessRegisteredAddress match {
        case Some(registeredBusinessAddress) if isAddressValid(registeredBusinessAddress) =>
          Future.successful(
            Ok(
              page(registeredBusinessAddress,
                   request.registration.organisationDetails.businessName.getOrElse(
                     "your organisation"
                   ),
                   commonRoutes.TaskListController.displayPage().url
              )
            )
          )
        case _ =>
          initialiseAddressLookup(addressLookupFrontendConnector,
                                  appConfig,
                                  routes.ConfirmBusinessAddressController.alfCallback(None),
                                  "addressLookup.business",
                                  request.registration.organisationDetails.businessName
          ).map(onRamp => Redirect(onRamp.redirectUrl))
      }

    }

  private def isAddressValid(address: Address) =
    Address.form().fillAndValidate(address).errors.isEmpty

  def alfCallback(id: Option[String]): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressLookupFrontendConnector.getAddress(
        id.getOrElse(throw new MissingAddressIdException)
      ).flatMap {
        // TODO: consider re-validating here as we do for contact address. The code there suggests that ALF might return
        //       an address which we consider invalid. Need to check this out!
        confirmedAddress =>
          update { reg =>
            reg.copy(organisationDetails =
              reg.organisationDetails.copy(businessRegisteredAddress =
                Some(Address(confirmedAddress))
              )
            )
          }.map { _ =>
            Redirect(commonRoutes.TaskListController.displayPage())
          }
      }
    }

}

case class RegistrationException(message: String) extends Exception
