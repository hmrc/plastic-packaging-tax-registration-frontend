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

package uk.gov.hmrc.plasticpackagingtax.registration.forms

import play.api.data.Forms.{number, text}
import play.api.data.{Forms, Mapping}
import play.api.libs.json.Json

import java.time.LocalDate
import scala.util.Try

case class Date(day: Option[Int], month: Option[Int], year: Option[Int]) {

  override def toString: String = asLocalDate.toString

  def asLocalDate: LocalDate = LocalDate.of(year.getOrElse(0), month.getOrElse(0), day.getOrElse(0))
}

object Date {
  implicit val format = Json.format[Date]

  val year  = "year"
  val month = "month"
  val day   = "day"

  val dateEmptyError       = "date.empty.error"
  val dayEmptyError        = "date.day.empty.error"
  val dayFormatError       = "date.day.format.error"
  val dayDecimalError      = "date.day.decimal.error"
  val dayOutOfRangeError   = "date.day.outOfRange.error"
  val monthOutOfRangeError = "date.month.outOfRange.error"

  private val dayIsWithinRange: Option[Int] => Boolean =
    day => !day.exists(d => d < 1 || d > 31)

  private val monthIsWithinRange: Option[Int] => Boolean =
    month => !month.exists(m => m < 1 || m > 12)

  private val isValidNumber: Option[String] => Boolean = input =>
    input.isEmpty || input.forall(i => Try(BigDecimal(i)).isSuccess)

  private val weightIsWholeNumber: Option[String] => Boolean = input =>
    input.isEmpty || !isValidNumber(input) || input.forall(i => Try(BigInt(i)).isSuccess)

  def mapping(): Mapping[Date] =
    Forms.mapping(
      day -> Forms.optional(text()).verifying(dayEmptyError, _.nonEmpty).verifying(dayFormatError,
                                                                                   isValidNumber
      ).verifying(dayDecimalError, weightIsWholeNumber)
        .transform[Option[Int]]((input: Option[String]) => input.map(BigInt(_).toInt),
                                int => int.map(_.toString)
        ).verifying(dayOutOfRangeError, dayIsWithinRange),
      month -> Forms.optional(number()).verifying(dateEmptyError, _.nonEmpty).verifying(
        monthOutOfRangeError,
        monthIsWithinRange
      ),
      year -> Forms.optional(number()).verifying(dateEmptyError, _.nonEmpty)
    )(Date.apply)(Date.unapply)

}
