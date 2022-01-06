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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

class GroupMemberContactDetailsSpec extends AnyWordSpec with Matchers with PptTestData {

  "Group member contact detail " should {

    "update name " when {
      "contact details exist " in {
        val member = groupMember.copy(contactDetails =
          Some(GroupMemberContactDetails(firstName = "Test", lastName = "User"))
        )
        val result = member.withGroupMemberName("Test1", "User2")
        result.firstName mustBe "Test1"
        result.lastName mustBe "User2"
      }
      "with no contact details " in {
        val member = groupMember.copy(contactDetails = None)
        val result = member.withGroupMemberName("Test1", "User2")
        result.firstName mustBe "Test1"
        result.lastName mustBe "User2"
      }
    }

    "update email address " when {
      "contact details exist " in {
        val member = groupMember.copy(contactDetails =
          Some(
            GroupMemberContactDetails(firstName = "Test",
                                      lastName = "User",
                                      email = Some("test@test.com")
            )
          )
        )
        val result = member.withGroupMemberEmail("t@t.com")
        result.email mustBe Some("t@t.com")
      }
      "with no contact details " in {
        val member = groupMember.copy(contactDetails = None)
        intercept[IllegalStateException] {
          member.withGroupMemberEmail("t@t.com")
        }
      }
    }
    "update phone number " when {
      "contact details exist " in {
        val member = groupMember.copy(contactDetails =
          Some(
            GroupMemberContactDetails(firstName = "Test",
                                      lastName = "User",
                                      phoneNumber = Some("0777123")
            )
          )
        )
        val result = member.withGroupMemberPhoneNumber("121212")
        result.phoneNumber mustBe Some("121212")
      }
      "with no contact details " in {
        val member = groupMember.copy(contactDetails = None)
        intercept[IllegalStateException] {
          member.withGroupMemberPhoneNumber("0777123")
        }
      }
    }
  }
}
