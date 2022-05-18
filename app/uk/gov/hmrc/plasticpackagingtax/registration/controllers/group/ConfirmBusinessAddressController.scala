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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{OVERSEAS_COMPANY_UK_BRANCH, OrgType}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Cacheable
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.services.{AddressCaptureConfig, AddressCaptureService}
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

  //TODO: Ensure redirectTo is sanitised to stop hijacking attack
  def displayPage(memberId: String, redirectTo: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      val member = request.registration.findMember(memberId).getOrElse(throw new IllegalStateException("Provided group memberId not found"))

      val orgType = member.organisationDetails
        .fold(throw new IllegalStateException("Failed to get org type for member"))(dets => OrgType.withName(dets.organisationType))

      member.addressDetails match {
        case registeredBusinessAddress if isAddressValidForOrgType(registeredBusinessAddress, Some(orgType)) =>
          Future.successful(Ok(page(registeredBusinessAddress, member.businessName, redirectTo)))
        case _ =>
          initialiseAddressLookup(memberId, request, redirectTo)
      }
    }

  private def isAddressValidForOrgType(address: Address, orgType: Option[OrgType]): Boolean =
    if (Address.validateAsInput(address).errors.isEmpty)
      orgType match {
        case Some(OVERSEAS_COMPANY_UK_BRANCH) => address.countryCode != "GB"
        case _                                => address.countryCode == "GB"
      }
    else
      false

  def changeBusinessAddress(memberId: String, redirectTo: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      initialiseAddressLookup(memberId, request, redirectTo)
    }

  private def initialiseAddressLookup(memberId: String, request: JourneyRequest[AnyContent], redirectTo: String)(implicit
    header: HeaderCarrier
  ): Future[Result] =
    addressCaptureService.initAddressCapture(
      AddressCaptureConfig(
        backLink = routes.ConfirmBusinessAddressController.displayPage(memberId, redirectTo).url,
        successLink = routes.ConfirmBusinessAddressController.addressCaptureCallback(memberId).url,
        alfHeadingsPrefix = "addressLookup.business",
        entityName = request.registration.findMember(memberId).map(_.businessName),
        pptHeadingKey = "addressCapture.business.heading",
        pptHintKey = None,
        forceUkAddress = true
      )
    )(request).map(redirect => Redirect(redirect))

  def addressCaptureCallback(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.getCapturedAddress().flatMap {
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
