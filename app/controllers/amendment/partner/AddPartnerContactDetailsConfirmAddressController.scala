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

package controllers.amendment.partner

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.EnrolledAuthAction
import forms.contact.Address
import models.registration.{
  AmendRegistrationUpdateService,
  Registration
}
import models.request.AmendmentJourneyAction
import services.{
  AddressCaptureConfig,
  AddressCaptureService
}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerContactDetailsConfirmAddressController @Inject() (
                                                                   authenticate: EnrolledAuthAction,
                                                                   journeyAction: AmendmentJourneyAction,
                                                                   addressCaptureService: AddressCaptureService,
                                                                   mcc: MessagesControllerComponents,
                                                                   registrationUpdater: AmendRegistrationUpdateService
)(implicit val ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.initAddressCapture(
        AddressCaptureConfig(
          backLink = routes.AddPartnerContactDetailsTelephoneNumberController.displayPage().url,
          successLink =
            routes.AddPartnerContactDetailsConfirmAddressController.addressCaptureCallback().url,
          alfHeadingsPrefix = "addressLookup.partner",
          entityName = request.registration.inflightPartner.map(_.name),
          pptHeadingKey = "addressCapture.contact.heading",
          pptHintKey = None,
          forceUkAddress = false
        )
      ).map(redirect => Redirect(redirect))
    }

  def addressCaptureCallback(): Action[AnyContent] =
    (authenticate andThen journeyAction).async { implicit request =>
      addressCaptureService.getCapturedAddress().flatMap {
        capturedAddress =>
          registrationUpdater.updateRegistration { registration =>
            update(capturedAddress)(registration)
          }.map {
            _ =>
              Redirect(routes.AddPartnerContactDetailsCheckAnswersController.displayPage())
          }
      }
    }

  private def update(address: Option[Address])(implicit registration: Registration): Registration =
    registration.withInflightPartner(
      registration.inflightPartner.map(_.withContactAddress(address))
    )

}