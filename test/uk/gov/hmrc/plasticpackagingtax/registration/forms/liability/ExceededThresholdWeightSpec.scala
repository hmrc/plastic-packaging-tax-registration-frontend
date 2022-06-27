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

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OldDate.day
import uk.gov.hmrc.plasticpackagingtax.registration.forms.YesNoValues

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

class ExceededThresholdWeightSpec extends PlaySpec {

  val mockMessages: Messages = mock[Messages]
  when(mockMessages.apply(anyString(), any())).thenReturn("some message")

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-06-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  val sut: Form[ExceededThresholdWeightAnswer] = new ExceededThresholdWeight(mockAppConfig, fakeClock).form()(mockMessages)

  // todo fix all test
  "The form" must {

    "bind correctly" when {
      "yes is provided" in {
       val boundForm = sut.bind(Map("answer" -> YesNoValues.YES,
          "exceeded-threshold-weight-date.day" -> "15",
          "exceeded-threshold-weight-date.month" -> "5",
          "exceeded-threshold-weight-date.year" -> "2022"))
        boundForm.value mustBe Some(ExceededThresholdWeightAnswer(true,Some(LocalDate.of(2022,5,15))))
        boundForm.errors mustBe Nil
      }

      "no is provided" in {//todo: break into two tests - date provided AND date isn't provided
        sut.bind(Map("answer" -> YesNoValues.YES)).value mustBe Some(false)
        sut.bind(Map("answer" -> YesNoValues.YES)).errors mustBe Nil
      }
    }

    "error correctly" when {
      "answer empty" in {
        sut.bind(Map.empty[String, String]).value mustBe None
        sut.bind(Map.empty[String, String]).errors.map(_.message) mustBe Seq(
          "liability.exceededThresholdWeight.question.empty.error"
        )
      }

      "answer is trash" in {
        sut.bind(Map("answer" -> "trash")).value mustBe None
        sut.bind(Map("answer" -> "trash")).errors.map(_.message) mustBe Seq(
          "liability.exceededThresholdWeight.question.empty.error"
        )
      }
    }
  }

  private def toMap(day: String, month: String, year: String): Map[String, String] =
    Map("expect-to-exceed-threshold-weight-date.day" -> day,
      "expect-to-exceed-threshold-weight-date.month" -> month,
      "expect-to-exceed-threshold-weight-date.year" -> year
    )
}
