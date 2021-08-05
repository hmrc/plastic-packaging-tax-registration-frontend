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

class LiabilityLiableDateSpec extends AnyWordSpec with Matchers {

  "LiableDate " should {
    "correctly apply" when {
      "'yes' is provided" in {
        val liabilityLiableDate = LiabilityLiableDate.apply("yes")
        liabilityLiableDate.answer mustBe Some(true)
      }

      "'no' is provided" in {
        val liabilityLiableDate = LiabilityLiableDate.apply("no")
        liabilityLiableDate.answer mustBe Some(false)
      }

      " neither 'yes' or 'no' are provided" in {
        val liabilityLiableDate = LiabilityLiableDate.apply("maybe")
        liabilityLiableDate.answer mustBe None
      }

      " string is empty" in {
        val liabilityLiableDate = LiabilityLiableDate.apply("")
        liabilityLiableDate.answer mustBe None
      }
    }

    "correctly unapply" when {
      "answer is 'Some(true)'" in {
        val liabilityLiableDate =
          LiabilityLiableDate.unapply(LiabilityLiableDate(Some(true)))
        liabilityLiableDate mustBe Some("yes")
      }

      "answer is 'Some(false)'" in {
        val liabilityLiableDate =
          LiabilityLiableDate.unapply(LiabilityLiableDate(Some(false)))
        liabilityLiableDate mustBe Some("no")
      }

      "answer is None" in {
        val liabilityLiableDate =
          LiabilityLiableDate.unapply(LiabilityLiableDate(None))
        liabilityLiableDate mustBe None
      }
    }
  }
}
