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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.{AppConfig, Features}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.{
  AuthAction,
  FormAction,
  SaveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.{
  AddressLookupConfigV2,
  AddressLookupOnRamp,
  MissingAddressIdException
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Cacheable,
  PrimaryContactDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.address_page
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  addressLookupFrontendConnector: AddressLookupFrontendConnector,
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  page: address_page
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.primaryContactDetails.address match {
        case Some(data) =>
          Future(Ok(page(Address.form().fill(data))))
        case _ =>
          if (request.isFeatureFlagEnabled(Features.isAddressLookupEnabled))
            initialiseAddressLookup.map(onRamp => Redirect(onRamp.redirectUrl))
          else
            Future(Ok(page(Address.form())))
      }
    }

  def submit(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      Address.form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[Address]) => Future.successful(BadRequest(page(formWithErrors))),
          address =>
            updateRegistration(address).map {
              case Right(_) =>
                FormAction.bindFromRequest match {
                  case SaveAndContinue =>
                    Redirect(routes.ContactDetailsCheckAnswersController.displayPage())
                  case _ =>
                    Redirect(commonRoutes.TaskListController.displayPage())
                }
              case Left(error) => throw error
            }
        )
    }

  def initialise(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      if (request.isFeatureFlagEnabled(Features.isAddressLookupEnabled))
        initialiseAddressLookup.map(onRamp => Redirect(onRamp.redirectUrl))
      else
        Future(Redirect(routes.ContactDetailsAddressController.displayPage()))
    }

  def update(id: Option[String]): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressLookupFrontendConnector.getAddress(
        id.getOrElse(throw new MissingAddressIdException)
      ).flatMap {
        confirmedAddress =>
          val updatedAddress = Address(confirmedAddress)
          // Address Lookup Service may return an address that is incompatible with PPT, so validate it again
          Address.form.fillAndValidate(updatedAddress).fold(
            (formWithErrors: Form[Address]) => Future.successful(BadRequest(page(formWithErrors))),
            address =>
              updateRegistration(Address(confirmedAddress)).map {
                case Right(_) =>
                  Redirect(routes.ContactDetailsCheckAnswersController.displayPage())
                case Left(error) => throw error
              }
          )
      }
    }

  private def updateRegistration(
    formData: Address
  )(implicit req: JourneyRequest[AnyContent]): Future[Either[ServiceError, Registration]] =
    update { registration =>
      registration.copy(primaryContactDetails = updateAddress(formData, registration))
    }

  private def updateAddress(address: Address, registration: Registration): PrimaryContactDetails =
    registration.primaryContactDetails.copy(address = Some(address))

  private def initialiseAddressLookup(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[AddressLookupOnRamp] =
    addressLookupFrontendConnector.initialiseJourney(
      AddressLookupConfigV2(routes.ContactDetailsAddressController.update(None), appConfig)
    )

}