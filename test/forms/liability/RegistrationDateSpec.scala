/*
 * Copyright 2023 HM Revenue & Customs
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

import base.unit.MessagesSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError
import play.api.test.FakeRequest
import forms.enrolment.RegistrationDate

import java.time.LocalDate

class RegistrationDateSpec extends MessagesSpec with Matchers {

  implicit val request = FakeRequest()

  private val year  = "date.year"
  private val month = "date.month"
  private val day   = "date.day"
  private val date  = "date"

  "RegistrationDate mapping validation rules" should {

    "is success" when {

      "registration date is valid (2021-10-01)" in {

        val input = Map(year -> "2021", month -> "10", day -> "01")

        val form = RegistrationDate.form().bind(input)
        form.errors.size mustBe 0
      }
    }

    "return errors" when {

      "provided with empty data" in {

        val input = Map(year -> "", month -> "", day -> "")
        val expectedErrors =
          Seq(FormError(day, "enrolment.registrationDate.value.error.empty")
          )

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "provided with year contains alphanumerical or special character" in {
      val input          = Map(year -> "20A#", month -> "7", day -> "13")
      val expectedErrors = Seq(FormError(year, "enrolment.registrationDate.value.error.format"))

      testFailedValidationErrors(input, expectedErrors)
    }

    "provided with month" which {

      "is less than 1" in {

        val input          = Map(year -> "2020", month -> "0", day -> "13")
        val expectedErrors = Seq(FormError(day, "enrolment.registrationDate.value.error.format"))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is more than 12" in {

        val input          = Map(year -> "2020", month -> "13", day -> "13")
        val expectedErrors = Seq(FormError(day, "enrolment.registrationDate.value.error.format"))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains alphanumerical or special character" in {

        val input          = Map(year -> "2020", month -> "C#", day -> "13")
        val expectedErrors = Seq(FormError(month, "enrolment.registrationDate.value.error.format"))

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "provided with day" which {

      "is less than 1" in {

        val input          = Map(year -> "2020", month -> "7", day -> "0")
        val expectedErrors = Seq(FormError(day, "enrolment.registrationDate.value.error.format"))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is more than 31" in {

        val input          = Map(year -> "2020", month -> "7", day -> "32")
        val expectedErrors = Seq(FormError(day, "enrolment.registrationDate.value.error.format"))

        testFailedValidationErrors(input, expectedErrors)
      }

      "contains alphanumerical or special character" in {

        val input          = Map(year -> "2020", month -> "7", day -> "C#")
        val expectedErrors = Seq(FormError(day, "enrolment.registrationDate.value.error.format"))

        testFailedValidationErrors(input, expectedErrors)
      }
    }

    "provided with date" which {

      "is in the future" in {

        val input =
          Map(year -> LocalDate.now().plusYears(1).getYear.toString, month -> "12", day -> "31")
        val expectedErrors = Seq(FormError(date, "enrolment.registrationDate.value.error.maxDate"))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is before 1-Jan-2021" in {

        val input =
          Map(year -> "2020", month -> "12", day -> "31")
        val expectedErrors = Seq(FormError(date, "enrolment.registrationDate.value.error.minDate"))

        testFailedValidationErrors(input, expectedErrors)
      }

      "is an invalid date e.g. (30-02-2020)" in {

        val input          = Map(year -> "2020", month -> "2", day -> "30")
        val expectedErrors = Seq(FormError(day, "enrolment.registrationDate.value.error.format"))

        testFailedValidationErrors(input, expectedErrors)
      }
    }
  }

  def testFailedValidationErrors(
    input: Map[String, String],
    expectedErrors: Seq[FormError]
  ): Unit = {
    val form = RegistrationDate.form().bind(input)
    expectedErrors.foreach(form.errors must contain(_))
  }

}
