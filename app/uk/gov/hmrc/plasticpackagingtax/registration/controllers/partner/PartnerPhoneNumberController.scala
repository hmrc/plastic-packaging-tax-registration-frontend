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

import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthAction
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.NewRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_phone_number_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnerPhoneNumberController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: partner_phone_number_page,
  registrationUpdateService: NewRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends PartnerPhoneNumberControllerBase(authenticate = authenticate,
                                             journeyAction = journeyAction,
                                             mcc = mcc,
                                             page = page,
                                             registrationUpdater = registrationUpdateService
    ) {

  def displayNewPartner(): Action[AnyContent] =
    doDisplay(None,
              partnerRoutes.PartnerContactNameController.displayNewPartner(),
              partnerRoutes.PartnerPhoneNumberController.submitNewPartner()
    )

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplay(Some(partnerId),
              partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
              partnerRoutes.PartnerPhoneNumberController.submitExistingPartner(partnerId)
    )

  def submitNewPartner(): Action[AnyContent] =
    doSubmit(None,
             partnerRoutes.PartnerContactNameController.displayNewPartner(),
             partnerRoutes.PartnerPhoneNumberController.submitNewPartner(),
             partnerRoutes.PartnerContactAddressController.captureNewPartner(),
             commonRoutes.TaskListController.displayPage()
    )

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    doSubmit(Some(partnerId),
             routes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
             routes.PartnerPhoneNumberController.submitExistingPartner(partnerId),
             routes.PartnerContactAddressController.captureExistingPartner(partnerId),
             partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partnerId)
    )

}
