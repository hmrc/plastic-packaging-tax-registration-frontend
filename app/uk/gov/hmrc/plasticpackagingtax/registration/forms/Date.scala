package uk.gov.hmrc.plasticpackagingtax.registration.forms

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class Date(date: LocalDate)

object Date {
  implicit val format: OFormat[Date] = Json.format[Date]
}
