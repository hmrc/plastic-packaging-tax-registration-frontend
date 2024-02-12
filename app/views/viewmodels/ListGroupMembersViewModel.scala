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

package views.viewmodels

import play.api.i18n.Messages
import controllers.amendment.group.routes
import models.registration.Registration

class ListGroupMembersViewModel(registration: Registration) {

  def listMembers(implicit messages: Messages): Seq[ListMember] =
    ListMember(
      name = registration.organisationDetails.businessName.getOrElse(
        throw new IllegalStateException("Missing Business Name")
      ),
      subHeading = Some(messages("amend.group.manage.representativeMember"))
    ) +:
      registration.groupDetail.toSeq.flatMap(
        o =>
          o.members.map(
            member =>
              ListMember(name = member.businessName,
                         change =
                           Some(routes.ContactDetailsCheckAnswersController.displayPage(member.id)),
                         remove =
                           if (o.members.length > 1)
                             Some(routes.ConfirmRemoveMemberController.displayPage(member.id))
                           else None
              )
          )
      )

}
