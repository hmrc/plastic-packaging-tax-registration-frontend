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

import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}
import forms.CommonFormValidators

case class Postcode(value: String)

object Postcode extends CommonFormValidators {
  implicit val format: OFormat[Postcode] = Json.format[Postcode]

  private val mapping = Forms.mapping(
    "value" -> text()
      .transform[String](postcode => postcode.toUpperCase, postcode => postcode)
      .verifying("enrolment.postcode.value.error.empty", isNonEmpty)
      .verifying("enrolment.postcode.value.error.regex", validatePostcode(10))
  )(Postcode.apply)(Postcode.unapply)

  def form(): Form[Postcode] = Form(mapping)
}
