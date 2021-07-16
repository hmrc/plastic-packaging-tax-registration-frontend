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
import play.api.Configuration

class EmailAllowedListProviderSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "EmailAllowedListProvider" should {

    val allowedEmail = "allowed@test.com"
    "load correctly from configuration" in {

      val config   = Configuration("allowedList.emails.0" -> allowedEmail)
      val provider = new EmailAllowedListProvider(config)
      provider.get() mustBe a[EmailAllowedList]
    }

    "trim spaces during loading" in {

      val config   = Configuration("allowedList.emails.0" -> s" $allowedEmail ")
      val provider = new EmailAllowedListProvider(config)
      provider.get().isAllowed(allowedEmail) mustBe true
    }

    "allow empty list" in {

      val config   = Configuration("allowedList.emails.0" -> "")
      val provider = new EmailAllowedListProvider(config)
      provider.get() mustBe a[EmailAllowedList]
    }

    "throw exception when there is not configuration key" in {

      val provider = new EmailAllowedListProvider(Configuration.empty)
      an[Exception] mustBe thrownBy {
        provider.get()
      }
    }
  }
}
