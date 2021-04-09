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

package uk.gov.hmrc.plasticpackagingtax.registration.forms

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfirmAddressSpec extends AnyWordSpec with Matchers {
  "ConfirmAddress" should {
    "correctly apply" when {
      "'yes' is provided" in {
        val confirmAddress = ConfirmAddress.apply("yes")
        confirmAddress.useRegisteredAddress mustBe Some(true)
      }

      "'no' is provided" in {
        val confirmAddress = ConfirmAddress.apply("no")
        confirmAddress.useRegisteredAddress mustBe Some(false)
      }

      " neither 'yes' or 'no' are provided" in {
        val confirmAddress = ConfirmAddress.apply("maybe")
        confirmAddress.useRegisteredAddress mustBe None
      }

      " string is empty" in {
        val confirmAddress = ConfirmAddress.apply("")
        confirmAddress.useRegisteredAddress mustBe None
      }
    }

    "correctly unapply" when {
      "userRegisteredAddress is 'Some(true)'" in {
        val confirmAddress = ConfirmAddress.unapply(ConfirmAddress(Some(true)))
        confirmAddress mustBe Some("yes")
      }

      "userRegisteredAddress is 'Some(false)'" in {
        val confirmAddress = ConfirmAddress.unapply(ConfirmAddress(Some(false)))
        confirmAddress mustBe Some("no")
      }

      "userRegisteredAddress is None" in {
        val confirmAddress = ConfirmAddress.unapply(ConfirmAddress(None))
        confirmAddress mustBe None
      }
    }
  }
}
