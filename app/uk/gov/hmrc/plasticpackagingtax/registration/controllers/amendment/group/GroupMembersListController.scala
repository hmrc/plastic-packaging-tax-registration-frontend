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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.AddOrganisation
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.list_group_members_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.ListMember
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.plasticpackagingtax.registration.controllers

import javax.inject.{Inject, Singleton}

@Singleton
class GroupMembersListController @Inject() (
  authenticate: AuthNoEnrolmentCheckAction,
  amendmentJourneyAction: AmendmentJourneyAction,
  mcc: MessagesControllerComponents,
  page: list_group_members_page
) extends FrontendController(mcc) with I18nSupport {

  def displayPage(): Action[AnyContent] =
    (authenticate andThen amendmentJourneyAction) { implicit request =>
      val registration = request.registration

      //todo do logic in controllers not views
      //todo when view is tested.
      val listMembers: Seq[ListMember] = Seq(
        ListMember(
          name = registration.organisationDetails.businessName.get,
          subHeading = Some(request.messages.apply("amend.group.manage.representativeMember")),
          change = None
        )) ++
        registration.groupDetail.toSeq.flatMap(_.members.map(member =>
          ListMember(
            name = member.businessName,
            change = Some(controllers.group.routes.ContactDetailsCheckAnswersController.displayPage(member.id)),
            remove = Some(routes.ConfirmRemoveMemberController.displayPage(member.id))
          ))
        )

      Ok(page(AddOrganisation.form(), request.registration))
    }

}
