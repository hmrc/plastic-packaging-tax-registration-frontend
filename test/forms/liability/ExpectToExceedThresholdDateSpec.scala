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

import config.AppConfig
import forms.liability.ExpectToExceedThresholdWeightDate.{beforeLiveDateError, dateEmptyError, dateFormattingError, dateOutOfRangeError, requiredKey}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate

class ExpectToExceedThresholdDateSpec extends PlaySpec {

  val message: Messages = mock[Messages]
  when(message.apply(anyString(), any())).thenReturn("some message")

  private val appConfig = mock[AppConfig]
  when(appConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))


  private  val sut = new ExpectToExceedThresholdWeightDate(appConfig)()(message)

  "ExpectToExceedThresholdWeight" must {
    "bind correctly" when {
      "date is provided" in {
        val boundForm = sut.bind(toMap("15", "5", "2022"))

        boundForm.value mustBe Some(LocalDate.of(2022, 5, 15))
        boundForm.errors mustBe Nil
      }
    }

    "error correctly" when {
      "answer empty" in {
        val boundForm = sut.bind(Map.empty[String, String])

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq(dateEmptyError)
      }

      "date is empty" in {
        val boundForm = sut.bind(toMap("", "", ""))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) must contain("liability.expectToExceedThresholdDate.none")
      }

      "only one date field is present" in {
        val boundForm = sut.bind(Map(
          "expect-to-exceed-threshold-weight-date.day" -> "15")
        )

        boundForm.value mustBe None
        boundForm.errors.map(_.message) must contain("liability.expectToExceedThresholdDate.two.required.fields")
      }

      "only two date fields is present" in {
        val boundForm = sut.bind(Map(
          "expect-to-exceed-threshold-weight-date.day" -> "15",
          "expect-to-exceed-threshold-weight-date.month" -> "5"
        ))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) must contain(requiredKey)
      }

      "date contain letters" in {
        val boundForm = sut.bind(toMap("av", "5", "2022"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq(dateFormattingError)
      }

      "date contain decimal" in {
        val boundForm = sut.bind(toMap("15", "5.6", "2022"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq(dateFormattingError)
      }

      "date is out of Range" in {
        val dateInFuture = LocalDate.now().plusDays(10)
        val boundForm = sut.bind(
          toMap(
            dateInFuture.getDayOfMonth.toString,
            dateInFuture.getMonthValue.toString,
            dateInFuture.getYear.toString)
        )

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq(dateOutOfRangeError)
      }

      "date is before go live date" in {
        val boundForm = sut.bind(toMap("15", "3", "2022"))

        boundForm.value mustBe None
        boundForm.errors mustBe Seq(FormError("expect-to-exceed-threshold-weight-date.day", beforeLiveDateError, Seq("some message")))
      }
    }
  }

  private def toMap(day: String, month: String, year: String): Map[String, String] =
    Map(
      "expect-to-exceed-threshold-weight-date.day" -> day,
      "expect-to-exceed-threshold-weight-date.month" -> month,
      "expect-to-exceed-threshold-weight-date.year" -> year
    )
}
