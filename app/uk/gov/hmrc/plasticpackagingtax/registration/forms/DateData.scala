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

package uk.gov.hmrc.plasticpackagingtax.registration.forms

import java.time.LocalDate

import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.libs.json.{Json, OFormat}

import scala.util.{Failure, Success, Try}

case class DateData(day: String, month: String, year: String) {
  def asLocalDate: LocalDate = LocalDate.of(year.toInt, month.toInt, day.toInt)
}

object DateData {
  implicit val format: OFormat[DateData] = Json.format[DateData]

  def validDay(errorKey: String = "date.day.error"): Constraint[String] =
    Constraint {
      data =>
        Try(data.toInt) match {
          case Success(day) if day > 31 || day < 1 => Invalid(errorKey)
          case Failure(_)                          => Invalid(errorKey)
          case _                                   => Valid
        }
    }

  def validMonth(errorKey: String = "date.month.error"): Constraint[String] =
    Constraint {
      data =>
        Try(data.toInt) match {
          case Success(month) if month > 12 || month < 1 => Invalid(errorKey)
          case Failure(_)                                => Invalid(errorKey)
          case _                                         => Valid
        }
    }

  def validYear(errorKey: String = "date.year.error"): Constraint[String] =
    Constraint {
      data =>
        Try(data.toInt) match {
          case Failure(_) => Invalid(errorKey)
          case _          => Valid
        }
    }

  def validDate(errorKey: String): Constraint[DateData] =
    Constraint {
      case data if Try(data.asLocalDate).isFailure => Invalid(errorKey)
      case _                                       => Valid
    }

  def maxDateToday(errorKey: String): Constraint[DateData] =
    Constraint {
      data =>
        Try(data.asLocalDate) match {
          case Success(date) if date.isAfter(LocalDate.now()) => Invalid(errorKey)
          case _                                              => Valid
        }
    }

  def minDate(minimum: LocalDate, errorKey: String): Constraint[DateData] =
    Constraint {
      data =>
        Try(data.asLocalDate) match {
          case Success(date) if date.isBefore(minimum) => Invalid(errorKey)
          case _                                       => Valid
        }
    }

}
