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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date.{
  dateDecimalError,
  day,
  dayEmptyError,
  dayFormatError,
  dayOutOfRangeError,
  month,
  monthEmptyError,
  monthFormatError,
  monthOutOfRangeError,
  year,
  yearEmptyError,
  yearFormatError
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityStartDate.{
  dateFormattingError,
  dateLowerLimit,
  dateOutOfRangeError
}

class LiabilityStartDateSpec extends AnyWordSpec with Matchers {

  "Date mapping validation rules" should {

    "is success" when {

      "is exactly one year after tax liability starts (01-04-2022)" in {
//TODO need to change the test when we get the exact dates
        val date = dateLowerLimit
        val input = Map("year" -> date.getYear.toString,
                        "month" -> date.getMonthValue.toString,
                        "day"   -> date.getDayOfMonth.toString
        )

        val form = LiabilityStartDate.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input = Map.empty[String, String]
        val expectedErrors =
          Seq(FormError(year, yearEmptyError),
              FormError(month, monthEmptyError),
              FormError(day, dayEmptyError)
          )

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "provided with year only" in {

      val input          = Map("year" -> "2003")
      val expectedErrors = Seq(FormError(month, monthEmptyError), FormError(day, dayEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with month only" in {

      val input          = Map("month" -> "7")
      val expectedErrors = Seq(FormError(year, yearEmptyError), FormError(day, dayEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with day only" in {

      val input          = Map("day" -> "13")
      val expectedErrors = Seq(FormError(year, yearEmptyError), FormError(month, monthEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with no year" in {

      val input          = Map("month" -> "7", "day" -> "13")
      val expectedErrors = Seq(FormError(year, yearEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with no month" in {

      val input          = Map("year" -> "2003", "day" -> "13")
      val expectedErrors = Seq(FormError(month, monthEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with no day" in {

      val input          = Map("year" -> "2003", "month" -> "7")
      val expectedErrors = Seq(FormError(day, dayEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with year" which {

      "contains alphanumerical or special character" in {

        val input          = Map("year" -> "20A#", "month" -> "7", "day" -> "13")
        val expectedErrors = Seq(FormError(year, yearFormatError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains decimal number" in {

        val input          = Map("year" -> "2003.01", "month" -> "7", "day" -> "13")
        val expectedErrors = Seq(FormError(year, dateDecimalError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains leading blank space" in {

        val input = Map("year" -> " 2022", "month" -> "4", "day" -> "1")
        val form  = LiabilityStartDate.form().bind(input)
        form.errors.size mustBe 0
      }

      "contains trailing blank space" in {

        val input = Map("year" -> "2022 ", "month" -> "4", "day" -> "1")
        val form  = LiabilityStartDate.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "provided with month" which {

      "is less than 1" in {

        val input          = Map("year" -> "2003", "month" -> "0", "day" -> "13")
        val expectedErrors = Seq(FormError("month", monthOutOfRangeError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is more than 12" in {

        val input          = Map("year" -> "2003", "month" -> "13", "day" -> "13")
        val expectedErrors = Seq(FormError("month", monthOutOfRangeError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains alphanumerical or special character" in {

        val input          = Map("year" -> "2003", "month" -> "C#", "day" -> "13")
        val expectedErrors = Seq(FormError(month, monthFormatError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains decimal number" in {

        val input          = Map("year" -> "2003", "month" -> "7.6", "day" -> "13")
        val expectedErrors = Seq(FormError(month, dateDecimalError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains leading blank space" in {
        val input = Map("year" -> "2022", "month" -> "   4", "day" -> "1")
        val form  = LiabilityStartDate.form().bind(input)
        form.errors.size mustBe 0
      }

      "contains trailing blank space" in {
        val input = Map("year" -> "2022", "month" -> " 4 ", "day" -> "1")
        val form  = LiabilityStartDate.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "provided with day" which {

      "is less than 1" in {

        val input          = Map("year" -> "2003", "month" -> "7", "day" -> "0")
        val expectedErrors = Seq(FormError("day", dayOutOfRangeError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is more than 31" in {

        val input          = Map("year" -> "2003", "month" -> "7", "day" -> "32")
        val expectedErrors = Seq(FormError("day", dayOutOfRangeError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is 31-st of February" in {

        val input          = Map("year" -> "2003", "month" -> "02", "day" -> "31")
        val expectedErrors = Seq(FormError("", dateFormattingError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains alphanumerical or special character" in {

        val input          = Map("year" -> "2003", "month" -> "7", "day" -> "C#")
        val expectedErrors = Seq(FormError(day, dayFormatError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains decimal number" in {

        val input          = Map("year" -> "2003", "month" -> "7", "day" -> "1.5")
        val expectedErrors = Seq(FormError(day, dateDecimalError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains leading blank space" in {

        val input = Map("year" -> "2022", "month" -> "4", "day" -> " 1")
        val form  = LiabilityStartDate.form().bind(input)
        form.errors.size mustBe 0
      }

      "contains trailing blank space" in {

        val input = Map("year" -> "2022", "month" -> "4", "day" -> "1  ")
        val form  = LiabilityStartDate.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "provided with date" which {

      "is 2022-03-31 (before PPT tax)" in {
        val date = dateLowerLimit.plusYears(1)
        val input = Map("year" -> date.getYear.toString,
                        "month" -> date.getMonthValue.toString,
                        "day"   -> date.getDayOfMonth.toString
        )
        val expectedErrors = Seq(FormError("", dateOutOfRangeError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is more than one year after tax liability starts (01-04-2022)" in {

        val date = dateLowerLimit.plusYears(1).plusDays(1)
        val input = Map("year" -> date.getYear.toString,
                        "month" -> date.getMonthValue.toString,
                        "day"   -> date.getDayOfMonth.toString
        )
        val expectedErrors = Seq(FormError("", dateOutOfRangeError))

        testFailedValidationErrors(input, expectedErrors)
      }
    }
  }

  def testFailedValidationErrors(
    input: Map[String, String],
    expectedErrors: Seq[FormError]
  ): Unit = {
    val form = LiabilityStartDate.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
  }

}
