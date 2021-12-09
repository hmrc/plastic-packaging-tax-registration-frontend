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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.liability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityWeight.{
  maxTotalKg,
  minTotalKg,
  totalKg,
  weightBelowThresholdError,
  weightEmptyError,
  weightOutOfRangeError
}

class LiabilityWeightTest extends AnyWordSpec with Matchers {

  "Liability weight validation rules" should {

    "return success" when {

      "is within range of 1 thousand and 100 million" in {

        val input = Map(totalKg -> "5555")

        val form = LiabilityWeight.form().bind(input)
        form.errors.size mustBe 0
      }

      "is exactly min allowed amount" in {

        val input = Map(totalKg -> minTotalKg.toString)

        val form = LiabilityWeight.form().bind(input)
        form.errors.size mustBe 0
      }

      "is exactly max allowed amount" in {

        val input = Map(totalKg -> maxTotalKg.toString)

        val form = LiabilityWeight.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input          = Map.empty[String, String]
        val expectedErrors = Seq(FormError(totalKg, weightEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains alphanumerical or special character" in {

        val input          = Map(totalKg -> "20A#")
        val expectedErrors = Seq(FormError(totalKg, "error.number"))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains total less than minimum allowed weight" in {

        val input          = Map(totalKg -> (minTotalKg - 1).toString)
        val expectedErrors = Seq(FormError("totalKg", weightBelowThresholdError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains total more than maximum allowed weight" in {

        val input          = Map(totalKg -> (maxTotalKg + 1).toString)
        val expectedErrors = Seq(FormError("totalKg", weightOutOfRangeError))

        testFailedValidationErrors(input, expectedErrors)
      }
    }
  }

  def testFailedValidationErrors(
    input: Map[String, String],
    expectedErrors: Seq[FormError]
  ): Unit = {
    val form = LiabilityWeight.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
  }

}
