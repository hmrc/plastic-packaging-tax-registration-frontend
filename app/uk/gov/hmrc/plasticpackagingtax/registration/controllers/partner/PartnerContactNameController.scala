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
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.NewRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{JourneyAction, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_member_name_page

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PartnerContactNameController @Inject() (
  authenticate: AuthAction,
  journeyAction: JourneyAction,
  mcc: MessagesControllerComponents,
  page: partner_member_name_page,
  registrationUpdateService: NewRegistrationUpdateService
)(implicit ec: ExecutionContext)
    extends PartnerContactNameControllerBase(authenticate = authenticate,
                                             journeyAction = journeyAction,
                                             mcc = mcc,
                                             page = page,
                                             registrationUpdater = registrationUpdateService
    ) {

  def displayNewPartner(): Action[AnyContent] =
    doDisplay(None,
              partnerRoutes.PartnerTypeController.displayNewPartner(),
              partnerRoutes.PartnerContactNameController.submitNewPartner()
    )

  def displayExistingPartner(partnerId: String): Action[AnyContent] =
    doDisplay(Some(partnerId),
              partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
              partnerRoutes.PartnerContactNameController.submitExistingPartner(partnerId)
    )

  def submitNewPartner(): Action[AnyContent] =
    doSubmit(None,
             partnerRoutes.PartnerTypeController.displayNewPartner(),
             partnerRoutes.PartnerContactNameController.submitNewPartner(),
             commonRoutes.TaskListController.displayPage()
    )

  def submitExistingPartner(partnerId: String): Action[AnyContent] =
    doSubmit(Some(partnerId),
             partnerRoutes.PartnerCheckAnswersController.displayExistingPartner(partnerId),
             partnerRoutes.PartnerContactNameController.submitExistingPartner(partnerId),
             partnerRoutes.PartnerContactNameController.displayExistingPartner(partnerId)
    )

  override def onwardCallNewPartner()(implicit request: JourneyRequest[AnyContent]): Call =
    request.registration.inflightPartner.map { partner =>
      if (isEditingNominatedPartner(request, partner))
        routes.PartnerJobTitleController.displayNewPartner()
      else
        routes.PartnerEmailAddressController.displayNewPartner()
    }.getOrElse(routes.PartnerEmailAddressController.displayNewPartner())

  override def onwardCallExistingPartner(
    partnerId: String
  )(implicit request: JourneyRequest[AnyContent]): Call =
    request.registration.findPartner(partnerId).map { partner =>
      val alreadyHasJobTitle = partner.contactDetails.flatMap(_.jobTitle).nonEmpty
      if (isEditingNominatedPartner(request, partner) || alreadyHasJobTitle)
        partnerRoutes.PartnerJobTitleController.displayExistingPartner(partnerId)
      else
        partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partnerId)
    }.getOrElse(partnerRoutes.PartnerEmailAddressController.displayExistingPartner(partnerId))

  private def isEditingNominatedPartner(request: JourneyRequest[AnyContent], partner: Partner) =
    request.registration.nominatedPartner.forall(_.id == partner.id)

}
