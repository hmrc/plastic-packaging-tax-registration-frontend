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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.i18n.Messages
import config.AppConfig
import forms.YesNoValues

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

class ExpectToExceedThresholdWeightSpec extends PlaySpec {
  val mockMessages: Messages = mock[Messages]
  when(mockMessages.apply(anyString(), any())).thenReturn("some message")

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-06-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  val formProvider: ExpectToExceedThresholdWeight = new ExpectToExceedThresholdWeight(mockAppConfig, fakeClock)

  private val sut: Form[ExpectToExceedThresholdWeightAnswer] =
    formProvider()(mockMessages)


  "ExpectToExceedThresholdWeight" must {
    "bind correctly" when {
      "yes is provided" in {
        val boundForm = sut.bind(toMap(YesNoValues.YES, "15", "5", "2022"))

        boundForm.value mustBe Some(
          ExpectToExceedThresholdWeightAnswer(true, Some(LocalDate.of(2022, 5, 15))))
        boundForm.errors mustBe Nil
      }

      "no is provided with No date" in {
        val boundForm = sut.bind(Map("answer" -> YesNoValues.NO))
        boundForm.value mustBe Some(ExpectToExceedThresholdWeightAnswer(false, None))
        boundForm.errors mustBe Nil
      }

      "no is provided with date" in {
        val boundForm = sut.bind(toMap(YesNoValues.NO, "15", "5", "2022"))

        boundForm.value mustBe Some(ExpectToExceedThresholdWeightAnswer(false, None))
        boundForm.errors mustBe Nil
      }
    }

    "error correctly" when {
      "answer empty" in {
        val boundForm = sut.bind(Map.empty[String, String])

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq("liability.expectToExceedThresholdWeight.question.empty.error")
      }

      "answer is trash" in {
        val boundForm = sut.bind(Map("answer" -> "trash"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq("liability.expectToExceedThresholdWeight.question.empty.error")
      }

      "date is empty" in {
        val boundForm = sut.bind(toMap(YesNoValues.YES, "", "", ""))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) must contain("liability.expectToExceedThreshold.date.none")
      }

      "only one date field is present" in {
        val boundForm = sut.bind(Map(
          "answer" -> YesNoValues.YES,
          "expect-to-exceed-threshold-weight-date.day" -> "15"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) must contain("liability.expectToExceedThreshold.two.required.fields")
      }

      "only two date fields is present" in {
        val boundForm = sut.bind(Map(
          "answer" -> YesNoValues.YES,
          "expect-to-exceed-threshold-weight-date.day" -> "15",
          "expect-to-exceed-threshold-weight-date.month" -> "5"
        ))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) must contain("liability.expectToExceedThreshold.one.field")
      }

      "date contain letters" in {
        val boundForm = sut.bind(toMap(YesNoValues.YES, "av", "5", "2022"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq("liability.expectToExceedThreshold.date.invalid")
      }

      "date contain decimal" in {
        val boundForm = sut.bind(toMap(YesNoValues.YES, "15", "5.6", "2022"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq("liability.expectToExceedThreshold.date.invalid")
      }

      "date is out of Range" in {
        val boundForm = sut.bind(toMap(YesNoValues.YES, "15", "6", "2022"))

        boundForm.value mustBe None
        boundForm.errors.map(_.message) mustBe Seq("liability.expectToExceedThreshold.date.future")
      }

      "date is before go live date" in {
        val boundForm = sut.bind(toMap(YesNoValues.YES, "15", "3", "2022"))

        boundForm.value mustBe None
        val msgCapture: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
        verify(mockMessages, atLeastOnce()).apply(msgCapture.capture(), any())

        msgCapture.getValue mustBe "liability.taxStartDate.realisedThresholdWouldBeExceeded.before.goLiveDate.error"
      }
    }
  }

  private def toMap(answer: String, day: String, month: String, year: String): Map[String, String] =
    Map("answer" -> answer,
      "expect-to-exceed-threshold-weight-date.day" -> day,
      "expect-to-exceed-threshold-weight-date.month" -> month,
      "expect-to-exceed-threshold-weight-date.year" -> year
    )

}
