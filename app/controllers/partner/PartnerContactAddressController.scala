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

import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.NotEnrolledAuthAction
import controllers.actions.getRegistration.GetRegistrationAction
import models.registration.NewRegistrationUpdateService
import services.AddressCaptureService

import scala.concurrent.ExecutionContext

@Singleton
class PartnerContactAddressController @Inject() (
                                                  authenticate: NotEnrolledAuthAction,
                                                  journeyAction: GetRegistrationAction,
                                                  addressCaptureService: AddressCaptureService,
                                                  mcc: MessagesControllerComponents,
                                                  registrationUpdater: NewRegistrationUpdateService
)(implicit val ec: ExecutionContext)
    extends PartnerContactAddressControllerBase(authenticate,
                                                journeyAction,
                                                addressCaptureService,
                                                mcc,
                                                registrationUpdater
    ) {

  def captureNewPartner(): Action[AnyContent] =
    doDisplayPage(None,
                  routes.PartnerPhoneNumberController.displayNewPartner(),
                  routes.PartnerContactAddressController.addressCaptureCallbackNewPartner()
    )

  def captureExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplayPage(Some(partnerId),
                  routes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
                  routes.PartnerContactAddressController.addressCaptureCallbackExistingPartner(
                    partnerId
                  )
    )

  def addressCaptureCallbackNewPartner(): Action[AnyContent] =
    onAddressCaptureCallback(None, routes.PartnerCheckAnswersController.displayNewPartner())

  def addressCaptureCallbackExistingPartner(partnerId: String): Action[AnyContent] =
    onAddressCaptureCallback(Some(partnerId),
                             routes.PartnerCheckAnswersController.displayExistingPartner(partnerId)
    )

}
