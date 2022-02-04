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
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.AddressLookupIntegration
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthActioning
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.MissingAddressIdException
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.RegistrationUpdater
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  JourneyRequest
}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

abstract class ContactDetailsConfirmAddressControllerBase(
  authenticate: AuthActioning,
  journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  addressLookupFrontendConnector: AddressLookupFrontendConnector,
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with AddressLookupIntegration with I18nSupport {

  protected def doDisplayPage(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      initialiseAddressLookup(addressLookupFrontendConnector,
                              appConfig,
                              getAlfCallback(memberId),
                              "addressLookup.member.contact",
                              request.registration.groupDetail.flatMap(_.businessName(memberId))
      ).map(onRamp => Redirect(onRamp.redirectUrl))
    }

  protected def getAlfCallback(memberId: String): Call

  protected def doAlfCallback(id: Option[String], memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressLookupFrontendConnector.getAddress(
        id.getOrElse(throw new MissingAddressIdException)
      ).flatMap {
        // TODO: consider re-validating here as we do for contact address. The code there suggests that ALF might return
        //       an address which we consider invalid. Need to check this out!
        confirmedAddress =>
          registrationUpdater.updateRegistration { registration =>
            registration.copy(groupDetail =
              registration.groupDetail.map(
                _.withUpdatedOrNewMember(
                  registration.findMember(memberId).map(
                    _.withUpdatedGroupMemberAddress(Address(confirmedAddress))
                  ).getOrElse(throw new IllegalStateException("Expected group member absent"))
                )
              )
            )
          }.map { registration =>
            Redirect(
              getSuccessfulRedirect(
                registration.findMember(memberId).map(_.id).getOrElse(
                  throw new IllegalStateException("Expected group member missing")
                )
              )
            )
          }
      }
    }

  protected def getSuccessfulRedirect(memberId: String): Call
}

case class RegistrationException(message: String) extends Exception
