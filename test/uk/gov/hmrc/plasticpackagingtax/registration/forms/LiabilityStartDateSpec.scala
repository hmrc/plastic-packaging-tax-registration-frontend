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
import play.api.data.FormError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date.{dateEmptyError, day, month, year}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityStartDate.{
  dateFormattingError,
  dateLowerLimit,
  dateOutOfRangeError
}

import java.time.LocalDate

class LiabilityStartDateSpec extends AnyWordSpec with Matchers {

  "Date mapping validation rules" should {

    "is success" when {

      "is exactly one year after tax liability starts (01-04-2022)" in {

        val date = dateLowerLimit.plusYears(1)
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
          Seq(FormError(year, dateEmptyError),
              FormError(month, dateEmptyError),
              FormError(day, dateEmptyError)
          )

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "provided with year only" in {

      val input          = Map("year" -> "2003")
      val expectedErrors = Seq(FormError(month, dateEmptyError), FormError(day, dateEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with month only" in {

      val input          = Map("month" -> "7")
      val expectedErrors = Seq(FormError(year, dateEmptyError), FormError(day, dateEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with day only" in {

      val input          = Map("day" -> "13")
      val expectedErrors = Seq(FormError(year, dateEmptyError), FormError(month, dateEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with no year" in {

      val input          = Map("month" -> "7", "day" -> "13")
      val expectedErrors = Seq(FormError(year, dateEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with no month" in {

      val input          = Map("year" -> "2003", "day" -> "13")
      val expectedErrors = Seq(FormError(month, dateEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with no day" in {

      val input          = Map("year" -> "2003", "month" -> "7")
      val expectedErrors = Seq(FormError(day, dateEmptyError))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with year" which {

      "contains alphanumerical or special character" in {

        val input          = Map("year" -> "20A#", "month" -> "7", "day" -> "13")
        val expectedErrors = Seq(FormError(year, "error.number"))

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "provided with month" which {

      "is less than 1" in {

        val input          = Map("year" -> "2003", "month" -> "0", "day" -> "13")
        val expectedErrors = Seq(FormError("", dateFormattingError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is more than 12" in {

        val input          = Map("year" -> "2003", "month" -> "13", "day" -> "13")
        val expectedErrors = Seq(FormError("", dateFormattingError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains alphanumerical or special character" in {

        val input          = Map("year" -> "2003", "month" -> "C#", "day" -> "13")
        val expectedErrors = Seq(FormError(month, "error.number"))

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "provided with day" which {

      "is less than 1" in {

        val input          = Map("year" -> "2003", "month" -> "7", "day" -> "0")
        val expectedErrors = Seq(FormError("", dateFormattingError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is more than 31" in {

        val input          = Map("year" -> "2003", "month" -> "7", "day" -> "32")
        val expectedErrors = Seq(FormError("", dateFormattingError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is 31-st of February" in {

        val input          = Map("year" -> "2003", "month" -> "02", "day" -> "31")
        val expectedErrors = Seq(FormError("", dateFormattingError))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains alphanumerical or special character" in {

        val input          = Map("year" -> "2003", "month" -> "7", "day" -> "C#")
        val expectedErrors = Seq(FormError(day, "error.number"))

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "provided with date" which {

      "is 2022-03-31 (before PPT tax)" in {

        val input          = Map("year" -> "1999", "month" -> "12", "day" -> "31")
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
