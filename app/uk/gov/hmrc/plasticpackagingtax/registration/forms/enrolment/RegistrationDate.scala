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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.DateData
import uk.gov.hmrc.plasticpackagingtax.registration.forms.DateData._

case class RegistrationDate(value: DateData)

object RegistrationDate {
  implicit val format: OFormat[RegistrationDate] = Json.format[RegistrationDate]

  def form(): Form[RegistrationDate] =
    Form[RegistrationDate](
      mapping(
        "date" -> mapping("day" -> text().verifying(validDay()),
                          "month" -> text().verifying(validMonth()),
                          "year" -> text().verifying(
                            validYear(2020, "enrolment.registrationDate.value.error.minYear")
                          )
        )(DateData.apply)(DateData.unapply).verifying(
          validDate("enrolment.registrationDate.value.error.format"),
          maxDateToday("enrolment.registrationDate.value.error.maxDate")
        )
      )(RegistrationDate.apply)(RegistrationDate.unapply)
    )

}
