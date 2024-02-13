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

package controllers.amendment.partner

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions.JourneyAction
import controllers.partner.PartnerPhoneNumberControllerBase
import models.registration.AmendRegistrationUpdateService
import views.html.partner.partner_phone_number_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerContactDetailsTelephoneNumberController @Inject() (
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: partner_phone_number_page,
  registrationUpdateService: AmendRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends PartnerPhoneNumberControllerBase(journeyAction = journeyAction.amend, mcc = mcc, page = page, registrationUpdater = registrationUpdateService) {

  def displayPage(): Action[AnyContent] =
    doDisplay(None, routes.AddPartnerContactDetailsEmailAddressController.displayPage(), routes.AddPartnerContactDetailsTelephoneNumberController.submit())

  def submit(): Action[AnyContent] =
    doSubmit(
      None,
      routes.AddPartnerContactDetailsEmailAddressController.displayPage(),
      routes.AddPartnerContactDetailsTelephoneNumberController.submit(),
      routes.AddPartnerContactDetailsConfirmAddressController.displayPage()
    )

}
