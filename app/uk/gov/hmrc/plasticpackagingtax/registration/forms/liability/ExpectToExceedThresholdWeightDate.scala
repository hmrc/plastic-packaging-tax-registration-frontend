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

import com.google.inject.Singleton
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.util.Try

@Singleton
class ExpectToExceedThresholdWeightDate @Inject() (appConfig: AppConfig, clock: Clock) {

  val emptyError          = "liability.expectToExceedThreshold.date.none"
  val dateFormattingError = "liability.expectToExceedThreshold.date.invalid"
  val dateOutOfRangeError = "liability.expectToExceedThreshold.date.future"

  private val dateLowerLimit: LocalDate = appConfig.goLiveDate

  private val isDateFormatValid: Date => Boolean = date =>
    Try(LocalDate.parse(date.toString)).isSuccess

  private val isDateInRange: Date => Boolean = date => {
    val localDate = LocalDate.parse(date.toString)
    !localDate.isBefore(dateLowerLimit) && !localDate.isAfter(LocalDate.now(clock))
  }

  def form(): Form[Date] =
    Form(
      Date.mapping()
        .verifying(dateFormattingError, isDateFormatValid)
        .verifying(dateOutOfRangeError, date => !isDateFormatValid(date) || isDateInRange(date))
    )

}
