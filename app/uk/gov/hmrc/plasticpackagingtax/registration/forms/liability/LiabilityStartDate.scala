/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date

import java.time.LocalDate
import scala.util.Try

object LiabilityStartDate {

  val dateFormattingError = "liabilityStartDate.formatting.error"
  val dateOutOfRangeError = "liabilityStartDate.outOfRange.error"

  private val dateUpperLimit: LocalDate = LocalDate.now()

  val dateLowerLimit = LocalDate.of(2022, 4, 1)

  private val isDateFormatValid: Date => Boolean = date =>
    Try(LocalDate.parse(date.toString)).isSuccess

  private val isDateInRange: Date => Boolean = date =>
    (LocalDate.parse(date.toString).isEqual(dateLowerLimit) || LocalDate.parse(
      date.toString
    ).isBefore(dateLowerLimit)) &&
      (LocalDate.parse(date.toString).isEqual(dateUpperLimit) || LocalDate.parse(
        date.toString
      ).isAfter(dateUpperLimit))

  def form(): Form[Date] =
    Form(
      Date.mapping()
        .verifying(dateFormattingError, isDateFormatValid)
        .verifying(dateOutOfRangeError, date => !isDateFormatValid(date) || isDateInRange(date))
    )

}
