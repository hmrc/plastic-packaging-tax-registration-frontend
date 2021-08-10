/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.plasticpackagingtax.registration.config.AllowedUser

class AllowedUsersSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "email allow list" when {
    val testEmail1 = "email1@test.com"
    val testEmail2 = "email2@test.com"
    "is empty" should {
      "allow everyone" in {
        val emailAllowedList = new AllowedUsers(Seq.empty)
        emailAllowedList.isAllowed(testEmail1) mustBe true
        emailAllowedList.isAllowed(testEmail2) mustBe true
      }
    }
    "has elements" should {
      val emailAllowedList = new AllowedUsers(Seq(AllowedUser(email = testEmail1)))
      "allow listed email" in {
        emailAllowedList.isAllowed(testEmail1) mustBe true
      }
      "disallow not listed email" in {
        emailAllowedList.isAllowed(testEmail2) mustBe false
      }
    }
  }
}
