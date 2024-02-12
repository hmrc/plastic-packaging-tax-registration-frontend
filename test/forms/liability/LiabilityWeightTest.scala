/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.liability

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError
import forms.liability.LiabilityWeight.{
  maxTotalKg,
  minTotalKg,
  totalKg,
  weightBelowThresholdError,
  weightDecimalError,
  weightEmptyError,
  weightFormatError,
  weightOutOfRangeError
}
import models.registration.LiabilityDetails

class LiabilityWeightTest extends AnyWordSpec with Matchers {

  "Liability weight validation rules" should {

    "return success" when {

      "is within range of 0 and 99 999 999 million" in {

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

      "amount contain a ," in {
        val input = Map(totalKg -> "10,000")

        val form = LiabilityWeight.form().bind(input)
        form.errors.size mustBe 0
        form.value.get.totalKg mustBe Some(10000L)
      }

      "amount contain a space" in {
        val input = Map(totalKg -> "10 000")

        val form = LiabilityWeight.form().bind(input)
        form.errors.size mustBe 0
        form.value.get.totalKg mustBe Some(10000L)
      }

      "amount contain a words" in {
        val input = Map(totalKg -> "ghkghkj10125hjghjk")

        val form = LiabilityWeight.form().bind(input)
        form.errors.size mustBe 0
        form.value.get.totalKg mustBe Some(10125L)
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input          = Map(totalKg -> "")
        val expectedErrors = Seq(FormError(totalKg, weightEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains words between digit" in {

        val input          = Map(totalKg -> "100ab12")
        val expectedErrors = Seq(FormError(totalKg, weightFormatError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains decimal weight" in {

        val input          = Map(totalKg -> "10000.1")
        val expectedErrors = Seq(FormError(totalKg, weightDecimalError))

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

      "contains leading blank space" in {

        val input = Map(totalKg -> "  5555")
        val form  = LiabilityWeight.form().bind(input)
        form.errors.size mustBe 0
      }

      "contains trailing blank space" in {

        val input =
          Map(totalKg -> s"${LiabilityDetails.minimumLiabilityWeightKg} ")
        val form = LiabilityWeight.form().bind(input)
        form.errors.size mustBe 0
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
