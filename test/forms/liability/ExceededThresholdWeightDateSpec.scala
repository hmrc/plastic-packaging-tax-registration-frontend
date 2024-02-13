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

import config.AppConfig
import forms.liability.ExceededThresholdWeightDate._
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate

class ExceededThresholdWeightDateSpec extends PlaySpec {

  val message: Messages = mock[Messages]
  when(message.apply(anyString(), any)).thenReturn("some message")

  private val appConfig = mock[AppConfig]
  when(appConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val sut = new ExceededThresholdWeightDate(appConfig)()(message)

  "ExceededThresholdWeightDate" should {
    "bind correctly" in {
      val boundForm = sut.bind(toMap("15", "5", "2022"))

      boundForm.value mustBe Some(LocalDate.of(2022, 5, 15))
      boundForm.errors mustBe Nil
    }

    "bind correctly when date contains spaces" in {
      val boundForm = sut.bind(toMap("15", "5", "202 2"))

      boundForm.value mustBe Some(LocalDate.of(2022, 5, 15))
      boundForm.errors mustBe Nil
    }

    "error correctly" when {
      "date is empty" in {
        val boundForm = sut.bind(Map.empty[String, String])

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq(dateEmptyError)
      }

      "only one date field is present" in {
        val boundForm = sut.bind(Map("exceeded-threshold-weight-date.day" -> "15"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) must contain(twoRequiredKey)
      }

      "only two date fields is present" in {
        val boundForm = sut.bind(Map("exceeded-threshold-weight-date.day" -> "15", "exceeded-threshold-weight-date.month" -> "5"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) must contain(requiredKey)
      }

      "date contain letter" in {
        val boundForm = sut.bind(toMap("qq", "5", "2022"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq(dateFormattingError)
      }

      "date contain decimal" in {
        val boundForm = sut.bind(toMap("15", "5.6", "2022"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq(dateFormattingError)
      }

      "date is out of Range" in {
        val dateInFuture = LocalDate.now.plusDays(10)
        val boundForm    = sut.bind(toMap(dateInFuture.getDayOfMonth.toString, dateInFuture.getMonthValue.toString, dateInFuture.getYear.toString))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq(dateOutOfRangeError)
      }

      "date is before go live date" in {
        val boundForm = sut.bind(toMap("15", "3", "2022"))

        boundForm.value mustBe None
        boundForm.errors mustBe Seq(FormError("exceeded-threshold-weight-date.day", Seq(isBeforeLiveDateError), Seq("some message")))
      }

    }
  }

  private def toMap(day: String, month: String, year: String): Map[String, String] =
    Map("exceeded-threshold-weight-date.day" -> day, "exceeded-threshold-weight-date.month" -> month, "exceeded-threshold-weight-date.year" -> year)

}
