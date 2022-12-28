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

package forms.enrolment

import java.time.LocalDate
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import forms.mappings.Mappings

import scala.util.Try

case class DateData(day: String, month: String, year: String) {
  def asLocalDate: LocalDate = LocalDate.of(year.toInt, month.toInt, day.toInt)
}

object DateData {
  implicit val dateFormat: OFormat[DateData] = Json.format[DateData]
}

case class RegistrationDate(value: DateData)

object RegistrationDate extends Mappings {
  implicit val regFormat: OFormat[RegistrationDate] = Json.format[RegistrationDate]

  val minRegistrationDate: LocalDate = LocalDate.parse("2021-01-01")

  def form()(implicit messages: Messages): Form[RegistrationDate] =
    Form[RegistrationDate](
      mapping(
        "date" -> localDate(
          "enrolment.registrationDate.value.error.empty",
          "enrolment.registrationDate.value.error.missing.values",
          "enrolment.registrationDate.value.error.missing.values",
          "enrolment.registrationDate.value.error.format"
        ).verifying(
          isTodayOrPast("enrolment.registrationDate.value.error.maxDate"),
          isOnOrAfterMinimumRegDate("enrolment.registrationDate.value.error.minDate")
        ))(pack)(unpack)
    )

  private def isTodayOrPast(errorKey: String): Constraint[LocalDate] = {
    val today = LocalDate.now()
    Constraint {
      case date if date.isEqual(today) || date.isBefore(today) => Valid
      case _ => Invalid(errorKey)
    }
  }

  private def isOnOrAfterMinimumRegDate(errorKey: String): Constraint[LocalDate] =
    Constraint {
      case date if date.isEqual(minRegistrationDate) || date.isAfter(minRegistrationDate) => Valid
      case _ => Invalid(errorKey)
    }

  private def pack(localDate: LocalDate) =
    new RegistrationDate(
      DateData(
        localDate.getDayOfMonth.toString,
        localDate.getMonthValue.toString,
        localDate.getYear.toString)
    )

  private def unpack(r: RegistrationDate): Option[LocalDate] =
    Try(r.value.asLocalDate).toOption

}
