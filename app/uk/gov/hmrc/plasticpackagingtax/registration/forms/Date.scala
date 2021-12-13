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

import play.api.data.Forms.number
import play.api.data.{Forms, Mapping}
import play.api.libs.json.Json

import java.time.LocalDate

case class Date(day: Option[Int], month: Option[Int], year: Option[Int]) {

  override def toString: String = asLocalDate.toString

  def asLocalDate: LocalDate = LocalDate.of(year.getOrElse(0), month.getOrElse(0), day.getOrElse(0))
}

object Date {
  implicit val format = Json.format[Date]

  val year  = "year"
  val month = "month"
  val day   = "day"

  val dateEmptyError     = "date.empty.error"
  val dayOutOfRangeError = "date.day.outOfRange.error"

  private val dayIsWithinRange: Option[Int] => Boolean =
    day => !day.exists(d => d < 1 || d > 31)

  def mapping(): Mapping[Date] =
    Forms.mapping(
      day -> Forms.optional(number()).verifying(dateEmptyError, _.nonEmpty).verifying(
        dayOutOfRangeError,
        dayIsWithinRange
      ),
      month -> Forms.optional(number()).verifying(dateEmptyError, _.nonEmpty),
      year  -> Forms.optional(number()).verifying(dateEmptyError, _.nonEmpty)
    )(Date.apply)(Date.unapply)

}
