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

import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import controllers.actions.JourneyAction
import controllers.partner.PartnerContactNameControllerBase
import models.registration.AmendRegistrationUpdateService
import models.request.JourneyRequest
import views.html.partner.partner_member_name_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddPartnerContactDetailsNameController @Inject() (
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: partner_member_name_page,
  registrationUpdateService: AmendRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends PartnerContactNameControllerBase(journeyAction = journeyAction.amend, mcc = mcc, page = page, registrationUpdater = registrationUpdateService) {

  def displayPage: Action[AnyContent] =
    doDisplay(None, routes.AddPartnerOrganisationDetailsTypeController.displayPage(), routes.AddPartnerContactDetailsNameController.submit)

  def submit: Action[AnyContent] =
    doSubmit(None, routes.AddPartnerOrganisationDetailsTypeController.displayPage(), routes.AddPartnerContactDetailsNameController.submit)

  override def onwardCallNewPartner(implicit request: JourneyRequest[AnyContent]): Call =
    routes.AddPartnerContactDetailsEmailAddressController.displayPage()

  override def onwardCallExistingPartner(partnerId: String)(implicit request: JourneyRequest[AnyContent]): Call =
    routes.AddPartnerContactDetailsEmailAddressController.displayPage()

}
