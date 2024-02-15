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

package forms.mappings

import config.AppConfig
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.reset
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.FormError
import play.api.i18n.Messages

import java.time.LocalDate

class LiabilityLocalDateFormatterSpec extends PlaySpec with BeforeAndAfterEach {
  private val message   = mock[Messages]
  private val appConfig = mock[AppConfig]

  private val formatter =
    new LiabilityLocalDateFormatter("emptyDateKey", "singleRequiredKey", "twoRequiredKey", "invalidKey", "dateOutOfRangeError", "isBeforeLiveDateError", appConfig)(message)

  override def beforeEach() = {
    super.beforeEach()

    reset(message)
  }

  "bind" should {
    "return a date" in {
      when(appConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-05"))

      val result = formatter.bind("input", Map("input.day" -> "4", "input.month" -> "5", "input.year" -> "2022"))

      result mustBe Right(LocalDate.of(2022, 5, 4))
    }
    "strip spaces from date" in {
      when(appConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-05"))

      val result = formatter.bind("input", Map("input.day" -> "4", "input.month" -> "5", "input.year" -> "202 2"))

      result mustBe Right(LocalDate.of(2022, 5, 4))
    }
    "trim spaces from date" in {
      when(appConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-05"))

      val result = formatter.bind("input", Map("input.day" -> "4 ", "input.month" -> " 5", "input.year" -> " 2022 "))

      result mustBe Right(LocalDate.of(2022, 5, 4))
    }

    "error" when {
      "date in the future" in {
        when(message.apply(anyString(), any)).thenReturn("message")
        val date = LocalDate.now.plusMonths(1)

        val result = formatter.bind("input", Map("input.day" -> date.getDayOfMonth.toString, "input.month" -> date.getMonthValue.toString, "input.year" -> date.getYear.toString))

        result mustBe Left(Seq(FormError(s"input.day", "dateOutOfRangeError", Seq("message"))))
      }

      "date is before date go live" in {

        when(appConfig.goLiveDate).thenReturn(LocalDate.parse("2023-02-01"))
        when(message.apply(anyString(), any)).thenReturn("message")

        val result = formatter.bind("input", Map("input.day" -> "5", "input.month" -> "1", "input.year" -> "2023"))

        result mustBe Left(Seq(FormError(s"input.day", "isBeforeLiveDateError", Seq("1 message 2023"))))

      }
    }
  }

}
