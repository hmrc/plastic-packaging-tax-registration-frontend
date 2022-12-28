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

package forms.group

import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import forms.CommonFormValidators

case class MemberName(firstName: String, lastName: String)

object MemberName extends CommonFormValidators {

  implicit val format = Json.format[MemberName]

  private val firstName = "firstName"

  private val lastName = "lastName"

  private val mapping =
    Forms.mapping(firstName ->
                    text()
                      .verifying(emptyError(firstName), isProvided)
                      .verifying(whiteSpaceError(firstName), isNoneWhiteSpace)
                      .verifying(lengthError(firstName), isNotExceedingMaxLength(_, 35))
                      .verifying(nonAlphabeticError(firstName), isValidName),
                  lastName ->
                    text()
                      .verifying(emptyError(lastName), isProvided)
                      .verifying(whiteSpaceError(lastName), isNoneWhiteSpace)
                      .verifying(lengthError(lastName), isNotExceedingMaxLength(_, 35))
                      .verifying(nonAlphabeticError(lastName), isValidName)
    )(MemberName.apply)(MemberName.unapply)

  def form(): Form[MemberName] = Form(mapping)

  private def emptyError(field: String) = s"contactDetails.member.${field}.error.empty"

  private def lengthError(field: String) =
    s"contactDetails.member.${field}.error.length"

  private def nonAlphabeticError(field: String) =
    s"contactDetails.member.${field}.error.specialCharacters"

  private def whiteSpaceError(field: String) =
    s"contactDetails.member.${field}.error.spaces"

}
