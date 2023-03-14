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

package controllers.organisation

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import connectors.RegistrationConnector
import controllers.actions.auth.RegistrationAuthAction
import controllers.actions.getRegistration.GetRegistrationAction
import controllers.{routes => commonRoutes}
import forms.contact.Address
import forms.organisation.OrgType.{OVERSEAS_COMPANY_UK_BRANCH, OrgType}
import models.registration.Cacheable
import models.request.JourneyRequest
import services.{AddressCaptureConfig, AddressCaptureService}
import views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmBusinessAddressController @Inject() (
                                                   authenticate: RegistrationAuthAction,
                                                   journeyAction: GetRegistrationAction,
                                                   override val registrationConnector: RegistrationConnector,
                                                   addressCaptureService: AddressCaptureService,
                                                   mcc: MessagesControllerComponents,
                                                   page: confirm_business_address
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val orgType = request.registration.organisationDetails.organisationType
      request.registration.organisationDetails.businessRegisteredAddress match {
        case Some(registeredBusinessAddress) if isAddressValidForOrgType(registeredBusinessAddress, orgType) =>
          Future.successful(
            Ok(
              page(
                registeredBusinessAddress,
                request.registration.organisationDetails.businessName.getOrElse("your organisation"),
                commonRoutes.TaskListController.displayPage().url
              )
            )
          )
        case _ =>
          orgType match {
            case Some(OVERSEAS_COMPANY_UK_BRANCH) => initialiseAddressLookup(request, forceUKAddress = false)
            case _ => initialiseAddressLookup(request, forceUKAddress = true)
          }
      }
    }

  def changeBusinessAddress(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      request.registration.organisationDetails.organisationType match {
        case Some(OVERSEAS_COMPANY_UK_BRANCH) => initialiseAddressLookup(request, forceUKAddress = false)
        case _ => initialiseAddressLookup(request, forceUKAddress = true)
      }
    }

  private def initialiseAddressLookup(request: JourneyRequest[AnyContent], forceUKAddress: Boolean)(
    implicit header: HeaderCarrier): Future[Result] =
    addressCaptureService.initAddressCapture(
      AddressCaptureConfig(
        backLink = routes.ConfirmBusinessAddressController.displayPage().url,
        successLink =
          routes.ConfirmBusinessAddressController.addressCaptureCallback().url,
        alfHeadingsPrefix = "addressLookup.business",
        entityName = request.registration.organisationDetails.businessName,
        pptHeadingKey = "addressCapture.business.heading",
        pptHintKey = None,
        forceUkAddress = forceUKAddress
      )
    )(request.authenticatedRequest).map(redirect => Redirect(redirect))

  private def isAddressValidForOrgType(address: Address, orgType: Option[OrgType]): Boolean =
    if (Address.validateAsInput(address).errors.isEmpty)
      orgType match {
        case Some(OVERSEAS_COMPANY_UK_BRANCH) => address.countryCode != "GB"
        case _                                => address.countryCode == "GB"
      }
    else
      false

  def addressCaptureCallback(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.getCapturedAddress()(request.authenticatedRequest).flatMap {
        capturedAddress =>
          update { reg =>
            reg.copy(organisationDetails =
              reg.organisationDetails.copy(businessRegisteredAddress = capturedAddress)
            )
          }.map { _ =>
            Redirect(commonRoutes.TaskListController.displayPage())
          }
      }
    }

}

case class RegistrationException(message: String) extends Exception
