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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityExpectedWeight.{
  answer,
  answerError,
  maxTotalKg,
  totalKg,
  weightBelowThresholdError,
  weightDecimalError,
  weightEmptyError,
  weightFormatError,
  weightLeadingBlankSpaceError,
  weightOutOfRangeError
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.LiabilityDetails

class LiabilityExpectedWeightTest extends AnyWordSpec with Matchers {

  "Liability expect weight validation rules" should {

    "return success" when {

      "is within range of 10 thousand and 99 999 999" in {

        val input = Map(answer -> "yes", totalKg -> "15000")

        val form = LiabilityExpectedWeight.form().bind(input)
        form.errors.size mustBe 0
      }

      "is exactly min allowed amount" in {

        val input =
          Map(answer -> "yes", totalKg -> LiabilityDetails.minimumLiabilityWeightKg.toString)

        val form = LiabilityExpectedWeight.form().bind(input)
        form.errors.size mustBe 0
      }

      "is exactly max allowed amount" in {

        val input = Map(answer -> "yes", totalKg -> maxTotalKg.toString)

        val form = LiabilityExpectedWeight.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input          = Map.empty[String, String]
        val expectedErrors = Seq(FormError(answer, answerError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "provided with 'yes' answer but no weight'" in {

        val input          = Map(answer -> "yes", totalKg -> "")
        val expectedErrors = Seq(FormError(totalKg, weightEmptyError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains non-numeric weight" in {

        val input          = Map(answer -> "yes", totalKg -> "20A#")
        val expectedErrors = Seq(FormError(totalKg, weightFormatError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains decimal weight" in {

        val input          = Map(answer -> "yes", totalKg -> "10000.1")
        val expectedErrors = Seq(FormError(totalKg, weightDecimalError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains total less than minimum allowed weight" in {

        val input =
          Map(answer -> "yes", totalKg -> (LiabilityDetails.minimumLiabilityWeightKg - 1).toString)
        val expectedErrors = Seq(FormError("totalKg", weightBelowThresholdError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains total more than maximum allowed weight" in {

        val input          = Map(answer -> "yes", totalKg -> (maxTotalKg + 1).toString)
        val expectedErrors = Seq(FormError("totalKg", weightOutOfRangeError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains leading blank space" in {

        val input =
          Map(answer -> "yes", totalKg -> (" " + LiabilityDetails.minimumLiabilityWeightKg))
        val expectedErrors = Seq(FormError("totalKg", weightLeadingBlankSpaceError))

        testFailedValidationErrors(input, expectedErrors)
      }
    }
  }

  def testFailedValidationErrors(
    input: Map[String, String],
    expectedErrors: Seq[FormError]
  ): Unit = {
    val form = LiabilityExpectedWeight.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
    form.errors.size must equal(1)
  }

}
