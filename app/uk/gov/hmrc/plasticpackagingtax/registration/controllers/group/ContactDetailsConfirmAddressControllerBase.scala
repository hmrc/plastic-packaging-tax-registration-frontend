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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthActioning
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.RegistrationUpdater
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

abstract class ContactDetailsConfirmAddressControllerBase(
  authenticate: AuthActioning,
  journeyAction: ActionRefiner[AuthenticatedRequest, JourneyRequest],
  addressCaptureService: AddressCaptureService,
  mcc: MessagesControllerComponents,
  registrationUpdater: RegistrationUpdater
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  protected def doDisplayPage(memberId: String, backLink: Call): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.initAddressCapture(
        AddressCaptureConfig(backLink = backLink.url,
                             successLink = getAddressCaptureCallback(memberId).url,
                             alfHeadingsPrefix = "addressLookup.partner",
                             entityName =
                               request.registration.findMember(memberId).map(_.businessName),
                             pptHeadingKey = "addressCapture.contact.heading",
                             pptHintKey = None
        )
      ).map(redirect => Redirect(redirect))
    }

  protected def getAddressCaptureCallback(memberId: String): Call

  protected def onAddressCaptureCallback(memberId: String): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.getCapturedAddress().flatMap {
        capturedAddress =>
          registrationUpdater.updateRegistration { registration =>
            registration.copy(groupDetail =
              registration.groupDetail.map(
                _.withUpdatedOrNewMember(
                  registration.findMember(memberId).map(
                    _.withUpdatedGroupMemberAddress(capturedAddress)
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
