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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.plasticpackagingtax.registration.config.AllowedUser

class AllowedUsersProviderSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "AllowedUsersProvider" should {

    val allowedUser = AllowedUser(email = "allowed@test.com")
    "load correctly from configuration" in {

      val config = Configuration("allowedUsers.0.email" -> "allowed@test.com",
                                 "allowedUsers.0.features.isPreLaunch" -> true
      )
      val provider = new AllowedUsersProvider(config)
      provider.get() mustBe a[AllowedUsers]
    }

    "trim spaces during loading" in {

      val config = Configuration("allowedUsers.0.email" -> " allowed@test.com ",
                                 "allowedUsers.0.features.isPreLaunch" -> true
      )
      val provider = new AllowedUsersProvider(config)
      provider.get().isAllowed(allowedUser.email) mustBe true
    }

    "allow empty list" in {

      val config   = Configuration("allowedUsers" -> Seq.empty)
      val provider = new AllowedUsersProvider(config)
      provider.get() mustBe a[AllowedUsers]
    }

    "throw exception when there is not configuration key" in {

      val provider = new AllowedUsersProvider(Configuration.empty)
      an[Exception] mustBe thrownBy {
        provider.get()
      }
    }
  }
}
