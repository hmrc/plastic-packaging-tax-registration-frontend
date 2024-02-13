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

import org.mockito.ArgumentMatchers.{any, anyString, refEq}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import spec.PptTestData
import controllers.amendment.group.routes
import models.registration.group.GroupMember
import models.registration.{GroupDetail, Registration}
import org.scalatestplus.mockito.MockitoSugar.mock

class ListGroupMembersViewModelSpec extends PlaySpec with PptTestData {

  val mockMessages: Messages = mock[Messages]
  when(mockMessages.apply(anyString(), any())).thenReturn("some message")

  val members                    = Seq(groupMember, groupMember)
  val registration: Registration = createRegistration(members)
  val sut                        = createViewModel(registration)

  "listMembers" must {
    "have the lead member first" in {
      val first = sut.listMembers(mockMessages).head

      first.name mustBe registration.organisationDetails.businessName.get
      first.change mustBe None
      first.remove mustBe None

      verify(mockMessages).apply(refEq("amend.group.manage.representativeMember"), any())
    }

    "list all the other members" in {
      val others = sut.listMembers(mockMessages).tail

      others.zip(members).map {
        case (other, member) =>
          other.name mustBe member.businessName
          other.change mustBe Some(routes.ContactDetailsCheckAnswersController.displayPage(member.id))
          other.remove mustBe Some(routes.ConfirmRemoveMemberController.displayPage(member.id))
          other.subHeading mustBe None
      }
    }

    "not contain remove button" when {
      "has two member only" in {
        val viewModel = createViewModel(createRegistration(Seq(groupMember)))

        viewModel.listMembers(mockMessages).foreach(_.remove mustBe None)
      }

      "has one member only" in {
        val viewModel = createViewModel(createRegistration(Seq.empty))

        viewModel.listMembers(mockMessages).foreach(_.remove mustBe None)
      }
    }
  }

  private def createRegistration(members: Seq[GroupMember]): Registration = {
    val groupDetail1 = GroupDetail(membersUnderGroupControl = Some(true), members = members)
    aRegistration(withGroupDetail(Some(groupDetail1)))
  }

  private def createViewModel(registration: Registration): ListGroupMembersViewModel =
    new ListGroupMembersViewModel(registration)

}
