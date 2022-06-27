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
import org.scalatest.Ignore
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

@Ignore
class ExceededThresholdWeightSpec extends PlaySpec {

  val mockMessages: Messages = mock[Messages]
  when(mockMessages.apply(anyString(), any())).thenReturn("some message")

  private val mockAppConfig = mock[AppConfig]
  private val fakeClock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  val sut: Form[ExceededThresholdWeightAnswer] = new ExceededThresholdWeight(mockAppConfig, fakeClock).form()(mockMessages)

  // todo fix all test
  "The form" must {

    "bind correctly" when {
      "yes is provided" in {
        val t = sut.fill(
          ExceededThresholdWeightAnswer(true, Some(LocalDate.of(2022, 5, 15))))
//          "answer" -> "yes",
//          "exceeded-threshold-weight-date" -> LocalDate.of(2022, 5, 15).toString
//        ))
        t.value.map(_.yesNo) mustBe Some(true)
        sut.bind(Map("answer" -> "yes")).errors mustBe Nil
      }

      "no is provided" in {
        sut.bind(Map("answer" -> "no")).value mustBe Some(false)
        sut.bind(Map("answer" -> "no")).errors mustBe Nil
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
}
