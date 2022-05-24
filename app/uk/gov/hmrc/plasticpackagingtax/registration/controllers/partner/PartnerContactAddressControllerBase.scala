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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthActioning
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  Registration,
  RegistrationUpdater
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.services.{
  AddressCaptureConfig,
  AddressCaptureService
}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

abstract class PartnerContactAddressControllerBase(
  val authenticate: AuthActioning,
  val journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  addressCaptureService: AddressCaptureService,
  mcc: MessagesControllerComponents,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  protected def doDisplayPage(
    partnerId: Option[String],
    backLink: Call,
    captureAddressCallback: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.initAddressCapture(
        AddressCaptureConfig(backLink = backLink.url,
                             successLink = captureAddressCallback.url,
                             alfHeadingsPrefix = "addressLookup.contact",
                             entityName = getPartner(partnerId).map(_.name),
                             pptHeadingKey = "addressCapture.contact.heading",
                             pptHintKey = None,
                             forceUkAddress = false
        )
      ).map(redirect => Redirect(redirect))
    }

  protected def onAddressCaptureCallback(
    partnerId: Option[String],
    successfulRedirect: Call
  ): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.getCapturedAddress().flatMap {
        capturedAddress =>
          registrationUpdater.updateRegistration { registration =>
            update(partnerId, capturedAddress)(registration)
          }.map {
            _ =>
              Redirect(successfulRedirect)
          }
      }
    }

  private def update(partnerId: Option[String], address: Option[Address])(implicit
    registration: Registration
  ): Registration =
    partnerId match {
      case Some(partnerId) =>
        registration.withUpdatedPartner(partnerId,
                                        partner =>
                                          partner.copy(contactDetails =
                                            partner.contactDetails.map(_.copy(address = address))
                                          )
        )
      case _ =>
        val updatedInflightPartner = registration.withInflightPartner(
          registration.inflightPartner.map(_.withContactAddress(address))
        )
        updatedInflightPartner.withPromotedInflightPartner()
    }

  private def getPartner(
    partnerId: Option[String]
  )(implicit request: JourneyRequest[_]): Option[Partner] =
    partnerId match {
      case Some(partnerId) => request.registration.findPartner(partnerId)
      case _               => request.registration.inflightPartner
    }

}
