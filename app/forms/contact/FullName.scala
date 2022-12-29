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

package forms.contact

import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}
import forms.CommonFormValidators

case class FullName(value: String)

object FullName extends CommonFormValidators {
  implicit val format: OFormat[FullName] = Json.format[FullName]

  private val fullName = "value"

  private val mapping = Forms.mapping(
    fullName ->
      text()
        .verifying(emptyError(fullName), isProvided)
        .verifying(whiteSpaceError(fullName), isNoneWhiteSpace)
        .verifying(lengthError(fullName), isNotExceedingMaxLength(_, 160))
        .verifying(nonAlphabeticError(fullName), isValidFullName)
  )(FullName.apply)(FullName.unapply)

  def form(): Form[FullName] = Form(mapping)

  private def emptyError(field: String) = s"primaryContactDetails.fullNamePage.${field}.error.empty"

  private def lengthError(field: String) =
    s"primaryContactDetails.fullNamePage.${field}.error.length"

  private def nonAlphabeticError(field: String) =
    s"primaryContactDetails.fullNamePage.${field}.error.specialCharacters"

  private def whiteSpaceError(field: String) =
    s"primaryContactDetails.fullNamePage.${field}.error.spaces"

}
