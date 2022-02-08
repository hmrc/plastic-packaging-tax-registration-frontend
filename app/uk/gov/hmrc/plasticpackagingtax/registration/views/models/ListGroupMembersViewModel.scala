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

package uk.gov.hmrc.plasticpackagingtax.registration.views.models

import play.api.i18n.Messages
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group.routes
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration

class ListGroupMembersViewModel(registration: Registration) {
  val groupMemberCount: String = registration.groupDetail.map(_.members.size).getOrElse(0).toString

  def listMembers(implicit messages: Messages): Seq[ListMember] =
    ListMember(name = registration.organisationDetails.businessName.get, //todo hmmmm
               subHeading = Some(messages("amend.group.manage.representativeMember"))
    ) +:
      registration.groupDetail.toSeq.flatMap(
        _.members.map(
          member =>
            ListMember(name = member.businessName,
                       change = Some(
                         group.routes.ContactDetailsCheckAnswersController.displayPage(member.id)
                       ),
                       remove = Some(routes.ConfirmRemoveMemberController.displayPage(member.id))
            )
        )
      )

}
