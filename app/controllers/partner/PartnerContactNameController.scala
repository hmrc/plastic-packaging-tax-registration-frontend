/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.actions.JourneyAction
import controllers.partner.{routes => partnerRoutes}
import models.registration.NewRegistrationUpdateService
import models.request.JourneyRequest
import play.api.mvc._
import views.html.partner.partner_member_name_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnerContactNameController @Inject() (
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: partner_member_name_page,
  registrationUpdateService: NewRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends PartnerContactNameControllerBase(
      journeyAction = journeyAction.register,
      mcc = mcc,
      page = page,
      registrationUpdater = registrationUpdateService
    ) {

  def displayNewPartner: Action[AnyContent] =
    doDisplay(
      None,
      partnerRoutes.PartnerTypeController.displayNewPartner(),
      partnerRoutes.PartnerContactNameController.submitNewPartner
    )

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplay(
      Some(partnerId),
      partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
      partnerRoutes.PartnerContactNameController.submitExistingPartner(partnerId)
    )

  def submitNewPartner: Action[AnyContent] =
    doSubmit(
      None,
      partnerRoutes.PartnerTypeController.displayNewPartner(),
      partnerRoutes.PartnerContactNameController.submitNewPartner
    )

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    doSubmit(
      Some(partnerId),
      partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
      partnerRoutes.PartnerContactNameController.submitExistingPartner(partnerId)
    )

  override def onwardCallNewPartner(implicit request: JourneyRequest[AnyContent]): Call =
    request.registration.inflightPartner.map { partner =>
      if (request.registration.isNominatedPartnerOrFirstInflightPartner(partner))
        routes.PartnerJobTitleController.displayNewPartner()
      else
        routes.PartnerEmailAddressController.displayNewPartner()
    }.getOrElse(routes.PartnerEmailAddressController.displayNewPartner())

  override def onwardCallExistingPartner(partnerId: String)(implicit request: JourneyRequest[AnyContent]): Call =
    request.registration.findPartner(partnerId).map { partner =>
      val alreadyHasJobTitle = partner.contactDetails.flatMap(_.jobTitle).nonEmpty
      if (request.registration.isNominatedPartner(Some(partner.id)) || alreadyHasJobTitle)
        partnerRoutes.PartnerJobTitleController.displayExistingPartner(partnerId)
      else
        partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partnerId)
    }.getOrElse(partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partnerId))

}
