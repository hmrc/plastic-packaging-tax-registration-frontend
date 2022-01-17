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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spec.PptTestData

class GroupMemberContactDetailsSpec extends AnyWordSpec with Matchers with PptTestData {

  "Group member contact detail" should {

    "update name" when {
      "contact details exist" in {
        val member = groupMember.copy(contactDetails =
          Some(GroupMemberContactDetails(firstName = "Test", lastName = "User"))
        )
        val result = member.withUpdatedGroupMemberName("Test1", "User2")
        result.contactDetails.map(_.firstName).get mustBe "Test1"
        result.contactDetails.map(_.lastName).get mustBe "User2"
      }
      "with no contact details" in {
        val member = groupMember.copy(contactDetails = None)
        val result = member.withUpdatedGroupMemberName("Test1", "User2")
        result.contactDetails.map(_.firstName).get mustBe "Test1"
        result.contactDetails.map(_.lastName).get mustBe "User2"
      }
    }

    "update email address" when {
      "contact details exist" in {
        val member = groupMember.copy(contactDetails =
          Some(
            GroupMemberContactDetails(firstName = "Test",
                                      lastName = "User",
                                      email = Some("test@test.com")
            )
          )
        )
        val result = member.withUpdatedGroupMemberEmail("t@t.com")
        result.contactDetails.map(_.email).get mustBe Some("t@t.com")
      }
      "with no contact details" in {
        val member = groupMember.copy(contactDetails = None)
        intercept[IllegalStateException] {
          member.withUpdatedGroupMemberEmail("t@t.com")
        }
      }
    }
    "update phone number" when {
      "contact details exist" in {
        val member = groupMember.copy(contactDetails =
          Some(
            GroupMemberContactDetails(firstName = "Test",
                                      lastName = "User",
                                      phoneNumber = Some("0777123")
            )
          )
        )
        val result = member.withUpdatedGroupMemberPhoneNumber("121212")
        result.contactDetails.map(_.phoneNumber).get mustBe Some("121212")
      }
      "with no contact details" in {
        val member = groupMember.copy(contactDetails = None)
        intercept[IllegalStateException] {
          member.withUpdatedGroupMemberPhoneNumber("0777123")
        }
      }
    }
    "update address" when {
      "contact details exist" in {
        val member = groupMember.copy(contactDetails =
          Some(
            GroupMemberContactDetails(firstName = "Test",
                                      lastName = "User",
                                      address = Some(addressDetails)
            )
          )
        )
        val result =
          member.withUpdatedGroupMemberAddress(addressDetails.copy(postCode = Some("AA1 1AA")))
        result.contactDetails.map(_.address.get.postCode).get mustBe Some("AA1 1AA")
      }
      "with no contact details" in {
        val member = groupMember.copy(contactDetails = None)
        intercept[IllegalStateException] {
          member.withUpdatedGroupMemberPhoneNumber("0777123")
        }
      }
    }
  }
}
