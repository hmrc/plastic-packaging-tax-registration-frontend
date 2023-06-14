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

package controllers.partner

import forms.contact.Address
import models.genericregistration.Partner
import models.registration.{Registration, RegistrationUpdater}
import models.request.JourneyRequest
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{AddressCaptureConfig, AddressCaptureService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

abstract class PartnerContactAddressControllerBase(
  val journeyAction: ActionBuilder[JourneyRequest, AnyContent],
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
    journeyAction.async { implicit request =>
      addressCaptureService.initAddressCapture(
        AddressCaptureConfig(backLink = backLink.url,
                             successLink = captureAddressCallback.url,
                             alfHeadingsPrefix = "addressLookup.contact",
                             entityName = getPartner(partnerId).map(_.name),
                             pptHeadingKey = "addressCapture.contact.heading",
                             pptHintKey = None,
                             forceUkAddress = false
        )
      )(request.authenticatedRequest).map(redirect => Redirect(redirect))
    }

  protected def onAddressCaptureCallback(
    partnerId: Option[String],
    successfulRedirect: Call
  ): Action[AnyContent] =
    journeyAction.async { implicit request =>
      addressCaptureService.getCapturedAddress()(request.authenticatedRequest).flatMap {
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
