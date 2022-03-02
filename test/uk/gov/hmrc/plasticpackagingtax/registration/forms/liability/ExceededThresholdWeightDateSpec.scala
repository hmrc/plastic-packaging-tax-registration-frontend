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
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.data.FormError
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date

import java.time.{Clock, Instant, LocalDate}
import java.util.TimeZone

class ExceededThresholdWeightDateSpec extends AnyWordSpec with Matchers {

  private val mockAppConfig =
    mock[AppConfig]

  when(mockAppConfig.goLiveDate).thenReturn(LocalDate.parse("2022-04-01"))

  private val fakeClock =
    Clock.fixed(Instant.parse("2022-05-01T12:00:00Z"), TimeZone.getDefault.toZoneId)

  private val exceededThresholdWeightDate =
    new ExceededThresholdWeightDate(mockAppConfig, fakeClock)

  "Exceeded threshold weight date" should {
    "pass validation 01/04/2022" in {
      exceededThresholdWeightDate().fill(Date(LocalDate.of(2022, 4, 1))).errors.size mustBe 0
    }

    "pass validation 01/05/2022" in {
      exceededThresholdWeightDate().fillAndValidate(
        Date(LocalDate.of(2022, 5, 1))
      ).errors.size mustBe 0
    }

    "fails validation 01/02/2022" in {
      val validationError =
        exceededThresholdWeightDate().fillAndValidate(Date(LocalDate.of(2022, 2, 1)))
      validationError.errors.size mustBe 1
      validationError.errors.mustBe(
        List(
          FormError("exceeded-threshold-weight-date",
                    List("liability.exceededThresholdWeightDate.outOfRange.error")
          )
        )
      )
    }

    "fails validation 01/06/2022" in {
      val validationError =
        exceededThresholdWeightDate().fillAndValidate(Date(LocalDate.of(2022, 6, 1)))
      validationError.errors.size mustBe 1
      validationError.errors.mustBe(
        List(
          FormError("exceeded-threshold-weight-date",
                    List("liability.exceededThresholdWeightDate.outOfRange.error")
          )
        )
      )
    }
  }
}
