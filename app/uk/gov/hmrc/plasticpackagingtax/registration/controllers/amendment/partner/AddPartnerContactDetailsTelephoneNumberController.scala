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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.PartnerPhoneNumberControllerBase
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.AmendRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_phone_number_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerContactDetailsTelephoneNumberController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  journeyAction: AmendmentJourneyAction,
  mcc: MessagesControllerComponents,
  page: partner_phone_number_page,
  registrationUpdateService: AmendRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends PartnerPhoneNumberControllerBase(authenticate = authenticate,
                                             journeyAction = journeyAction,
                                             mcc = mcc,
                                             page = page,
                                             registrationUpdater = registrationUpdateService
    ) {

  def displayPage(): Action[AnyContent] =
    doDisplay(None,
              routes.AddPartnerContactDetailsEmailAddressController.displayPage(),
              routes.AddPartnerContactDetailsTelephoneNumberController.submit()
    )

  def submit(): Action[AnyContent] =
    doSubmit(None,
             routes.AddPartnerContactDetailsEmailAddressController.displayPage(),
             routes.AddPartnerContactDetailsTelephoneNumberController.submit(),
             routes.AddPartnerContactDetailsConfirmAddressController.displayPage(),
             routes.PartnersListController.displayPage()
    )

}