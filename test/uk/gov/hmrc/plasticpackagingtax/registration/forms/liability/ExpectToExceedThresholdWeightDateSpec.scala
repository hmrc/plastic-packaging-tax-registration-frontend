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

import org.mockito.Mockito.when
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

class ExpectToExceedThresholdWeightDateSpec extends AnyWordSpec with Matchers {

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  private val expectToExceedThresholdWeightDate =
    new ExpectToExceedThresholdWeightDate(mockAppConfig, fakeClock)

  "Expect To Exceed Threshold Weight Date" should {
    "reject empty dates" in {
      reject("", "", "")
    }
    "reject partially complete dates" in {
      reject("", "1", "2022")
      reject("1", "", "2022")
      reject("1", "1", "")
    }
    "reject 31-03-2022 (day before tax start date)" in {
      reject("31", "3", "2022")
    }
    "reject 02-05-2022 (tomorrow)" in {
      reject("2", "5", "2022")
    }

    "accept 01-04-2022 (tax start date)" in {
      accept("1", "4", "2022")
    }
    "accept 02-04-2022 (day after tax start date)" in {
      accept("2", "4", "2022")
    }
    "accept 01-05-2022 (today)" in {
      accept("1", "5", "2022")
    }
  }

  private def reject(day: String, month: String, year: String): Assertion =
    expectToExceedThresholdWeightDate().bind(toMap(day, month, year)).errors.nonEmpty mustBe true

  private def accept(day: String, month: String, year: String): Assertion =
    expectToExceedThresholdWeightDate().bind(toMap(day, month, year)).errors.isEmpty mustBe true

  private def toMap(day: String, month: String, year: String): Map[String, String] =
    Map("expect-to-exceed-threshold-weight-date.day"   -> day,
        "expect-to-exceed-threshold-weight-date.month" -> month,
        "expect-to-exceed-threshold-weight-date.year"  -> year
    )

}
