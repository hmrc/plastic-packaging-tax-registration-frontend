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

import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.NewRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.services.AddressCaptureService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ContactDetailsConfirmAddressController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  addressCaptureService: AddressCaptureService,
  mcc: MessagesControllerComponents,
  registrationUpdater: NewRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends ContactDetailsConfirmAddressControllerBase(authenticate,
                                                       journeyAction,
                                                       addressCaptureService,
                                                       mcc,
                                                       registrationUpdater
    ) {

  def displayPage(memberId: String): Action[AnyContent] =
    doDisplayPage(memberId, routes.ContactDetailsTelephoneNumberController.displayPage(memberId))

  def addressCaptureCallback(memberId: String): Action[AnyContent] =
    onAddressCaptureCallback(memberId)

  override protected def getAddressCaptureCallback(memberId: String): Call =
    routes.ContactDetailsConfirmAddressController.addressCaptureCallback(memberId)

  override protected def getSuccessfulRedirect(memberId: String): Call =
    routes.ContactDetailsCheckAnswersController.displayPage(memberId)

}
