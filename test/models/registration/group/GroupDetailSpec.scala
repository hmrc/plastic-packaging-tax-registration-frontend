/*
 * Copyright 2023 HM Revenue & Customs
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

package models.registration.group

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import forms.contact.Address.UKAddress
import models.registration.GroupDetail
import views.viewmodels.TaskStatus

class GroupDetailSpec extends AnyWordSpec with Matchers {

  "Group detail" should {

    "be NOT_STARTED" when {
      "members is empty" in {
        val groupDetail = GroupDetail()
        groupDetail.status mustBe TaskStatus.NotStarted
      }
    }

    "be IN_PROGRESS" when {
      "there is a member with no contact details" in {
        val groupDetail = GroupDetail().withUpdatedOrNewMember(aGroupMember("Barbie Plastic Ltd")
          .copy(contactDetails = None))
        groupDetail.status mustBe TaskStatus.InProgress
      }
    }

    "be COMPLETED" when {
      "members are present" in {
        val groupDetail = GroupDetail(members = Seq(aGroupMember("Subsidiary 1")))
        groupDetail.status mustBe TaskStatus.Completed
      }
    }

    "obtain business name" in {
      val sub1        = aGroupMember("Subsidiary 1")
      val sub2        = aGroupMember("Subsidiary 2")
      val groupDetail = GroupDetail(members = Seq(sub1, sub2))

      groupDetail.businessName(sub1.id) mustBe Some(sub1.businessName)
      groupDetail.businessName(sub2.id) mustBe Some(sub2.businessName)
    }

    "update existing member" in {
      val sub1        = aGroupMember("Subsidiary 1")
      val sub2        = aGroupMember("Subsidiary 2")
      val groupDetail = GroupDetail(members = Seq(sub1, sub2))

      val updatedCust1Id     = s"${sub1.customerIdentification1}XXX"
      val updatedSub1        = sub1.copy(customerIdentification1 = updatedCust1Id)
      val updatedGroupDetail = groupDetail.withUpdatedOrNewMember(updatedSub1)

      updatedGroupDetail.members.size mustBe 2
      updatedGroupDetail.members.head.customerIdentification1 mustBe updatedCust1Id
    }

    "add new member" in {
      val sub1        = aGroupMember("Subsidiary 1")
      val sub2        = aGroupMember("Subsidiary 2")
      val groupDetail = GroupDetail(members = Seq(sub1, sub2))

      val sub3               = aGroupMember("Subsidiary 3")
      val updatedGroupDetail = groupDetail.withUpdatedOrNewMember(sub3)

      updatedGroupDetail.members.size mustBe 3
      updatedGroupDetail.members(2).id mustBe sub3.id
    }
  }

  private def aGroupMember(name: String) =
    GroupMember(
      customerIdentification1 = "cid1",
      customerIdentification2 = Some("cid2"),
      organisationDetails = Some(OrganisationDetails(organisationType = "UK Company", organisationName = name, businessPartnerId = Some("BP-123"))),
      contactDetails = Some(
        GroupMemberContactDetails(
          firstName = "John",
          lastName = "Benkson",
          phoneNumber = Some("07875234567"),
          email = Some("john@ppt.com"),
          address = Some(anAddress())
        )
      ),
      addressDetails = anAddress()
    )

  private def anAddress() =
    new UKAddress(
      addressLine1 = "addressLine1",
      addressLine2 = Some("addressLine2"),
      addressLine3 = Some("addressLine3"),
      townOrCity = "Wakefield",
      postCode = "WF15 4HD"
    )

}
