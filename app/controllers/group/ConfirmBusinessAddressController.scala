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

package controllers.group

import connectors.RegistrationConnector
import controllers.actions.JourneyAction
import forms.contact.Address
import forms.organisation.OrgType
import forms.organisation.OrgType.{OVERSEAS_COMPANY_UK_BRANCH, OrgType}
import models.addresslookup.CountryCode.GB
import models.registration.Cacheable
import models.request.JourneyRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AddressCaptureConfig, AddressCaptureService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.URLSanitisationUtils
import views.html.organisation.confirm_business_address

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmBusinessAddressController @Inject() (
  journeyAction: JourneyAction,
  override val registrationConnector: RegistrationConnector,
  addressCaptureService: AddressCaptureService,
  mcc: MessagesControllerComponents,
  page: confirm_business_address
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with Cacheable with I18nSupport {

  def displayPage(memberId: String, redirectTo: String): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      val member = request.registration.findMember(memberId).getOrElse(throw new IllegalStateException("Provided group memberId not found"))

      val orgType = member.organisationDetails
        .fold(throw new IllegalStateException("Failed to get org type for member"))(dets => OrgType.withName(dets.organisationType))

      URLSanitisationUtils.asRelativeUrl(redirectTo).fold(throw new IllegalStateException(s"Redirect to url of $redirectTo was invalid")) { url =>
        member.addressDetails match {
          case registeredBusinessAddress if isAddressValidForOrgType(registeredBusinessAddress, Some(orgType)) =>
            Future.successful(Ok(page(registeredBusinessAddress, member.businessName, url)))
          case _ =>
            orgType match {
              case OVERSEAS_COMPANY_UK_BRANCH => initialiseAddressLookup(memberId, request, redirectTo, forceUKAddress = false)
              case _                          => initialiseAddressLookup(memberId, request, redirectTo, forceUKAddress = true)
            }
        }
      }
    }

  private def isAddressValidForOrgType(address: Address, orgType: Option[OrgType]): Boolean =
    if (Address.validateAsInput(address).errors.isEmpty)
      orgType match {
        case Some(OVERSEAS_COMPANY_UK_BRANCH) => address.countryCode != GB
        case _                                => address.countryCode == GB
      }
    else
      false

  def changeBusinessAddress(memberId: String, redirectTo: String): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      val res = for {
        member     <- request.registration.findMember(memberId)
        orgDetails <- member.organisationDetails
        orgType = orgDetails.organisationType
      } yield
        if (orgType == OrgType.value(OVERSEAS_COMPANY_UK_BRANCH))
          initialiseAddressLookup(memberId, request, redirectTo, forceUKAddress = false)
        else
          initialiseAddressLookup(memberId, request, redirectTo, forceUKAddress = true)

      res.getOrElse(initialiseAddressLookup(memberId, request, redirectTo, forceUKAddress = false))
    }

  private def initialiseAddressLookup(memberId: String, request: JourneyRequest[AnyContent], redirectTo: String, forceUKAddress: Boolean): Future[Result] =
    addressCaptureService.initAddressCapture(
      AddressCaptureConfig(
        backLink = routes.ConfirmBusinessAddressController.displayPage(memberId, redirectTo).url,
        successLink = routes.ConfirmBusinessAddressController.addressCaptureCallback(memberId).url,
        alfHeadingsPrefix = "addressLookup.business",
        entityName = request.registration.findMember(memberId).map(_.businessName),
        pptHeadingKey = "addressCapture.business.heading",
        pptHintKey = None,
        forceUkAddress = false
      )
    )(request.authenticatedRequest).map(redirect => Redirect(redirect))

  def addressCaptureCallback(memberId: String): Action[AnyContent] =
    journeyAction.register.async { implicit request =>
      addressCaptureService.getCapturedAddress()(request.authenticatedRequest).flatMap {
        capturedAddress =>
          update { reg =>
            reg.withUpdatedMemberAddress(memberId, capturedAddress.getOrElse(throw new IllegalStateException("No captured address")))
          }
      }.map { _ =>
        Redirect(routes.ContactDetailsNameController.displayPage(memberId))
      }
    }

}

case class RegistrationException(message: String) extends Exception
