/*
 * Copyright 2024 HM Revenue & Customs
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

import java.util.regex.Pattern

case class PptReference(value: String)

object PptReference extends CommonFormValidators {
  implicit val format: OFormat[PptReference] = Json.format[PptReference]

  private val PPT_REGEX = Pattern.compile("^X[A-Z]PPT000[0-9]{7}$")

  private val mapping = Forms.mapping(
    "value" -> text()
      .verifying("enrolment.pptReference.value.error.empty", isNonEmpty)
      .verifying("enrolment.pptReference.value.error.regex", pptReference => !isNonEmpty(pptReference) || isMatchingPattern(pptReference, PPT_REGEX))
  )(PptReference.apply)(PptReference.unapply)

  def form(): Form[PptReference] = Form(mapping)
}
