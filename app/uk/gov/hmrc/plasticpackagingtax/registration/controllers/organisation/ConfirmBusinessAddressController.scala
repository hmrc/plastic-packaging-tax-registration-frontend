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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.services.{
  AddressCaptureConfig,
  AddressCaptureService
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmBusinessAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  addressCaptureService: AddressCaptureService,
  mcc: MessagesControllerComponents,
  page: confirm_business_address
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

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
          initialiseAddressLookup(request)
      }
    }

  def changeBusinessAddress(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      initialiseAddressLookup(request)
    }

  private def initialiseAddressLookup(
    request: JourneyRequest[AnyContent]
  )(implicit header: HeaderCarrier): Future[Result] =
    addressCaptureService.initAddressCapture(
      AddressCaptureConfig(backLink = routes.ConfirmBusinessAddressController.displayPage().url,
                           successLink =
                             routes.ConfirmBusinessAddressController.addressCaptureCallback().url,
                           alfHeadingsPrefix = "addressLookup.business",
                           entityName = request.registration.organisationDetails.businessName,
                           pptHeadingKey = "addressLookup.contact.lookup.heading",
                           pptHintKey = None
      )
    )(request).map(redirect => Redirect(redirect))

  private def isAddressValid(address: Address) =
    Address.form().fillAndValidate(address).errors.isEmpty

  def addressCaptureCallback(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.getCapturedAddress().flatMap {
        capturedAddress =>
          update { reg =>
            reg.copy(organisationDetails =
              reg.organisationDetails.copy(businessRegisteredAddress =
                capturedAddress
              )
            )
          }.map { _ =>
            Redirect(commonRoutes.TaskListController.displayPage())
          }
      }
    }

}

case class RegistrationException(message: String) extends Exception
