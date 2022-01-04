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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.liability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ExpectToExceedThresholdWeightSpec extends AnyWordSpec with Matchers {
  "ProcessMoreWeight" should {
    "correctly apply" when {
      "'yes' is provided" in {
        val processMoreWeight = ExpectToExceedThresholdWeight.apply("yes")
        processMoreWeight.answer mustBe Some(true)
      }

      "'no' is provided" in {
        val processMoreWeight = ExpectToExceedThresholdWeight.apply("no")
        processMoreWeight.answer mustBe Some(false)
      }

      " neither 'yes' or 'no' are provided" in {
        val processMoreWeight = ExpectToExceedThresholdWeight.apply("maybe")
        processMoreWeight.answer mustBe None
      }

      " string is empty" in {
        val processMoreWeight = ExpectToExceedThresholdWeight.apply("")
        processMoreWeight.answer mustBe None
      }
    }

    "correctly unapply" when {
      "answer is 'Some(true)'" in {
        val processMoreWeight =
          ExpectToExceedThresholdWeight.unapply(ExpectToExceedThresholdWeight(Some(true)))
        processMoreWeight mustBe Some("yes")
      }

      "answer is 'Some(false)'" in {
        val processMoreWeight =
          ExpectToExceedThresholdWeight.unapply(ExpectToExceedThresholdWeight(Some(false)))
        processMoreWeight mustBe Some("no")
      }

      "answer is None" in {
        val processMoreWeight =
          ExpectToExceedThresholdWeight.unapply(ExpectToExceedThresholdWeight(None))
        processMoreWeight mustBe None
      }
    }
  }
}
