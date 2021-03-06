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

import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}

case class FullName(firstName: String, lastName: String)

object FullName extends CommonFormValidators {
  implicit val format: OFormat[FullName] = Json.format[FullName]

  val allowedChars: Option[String] = Some(".-'")

  private val firstName = "firstName"

  private val lastName = "lastName"

  private val mapping = Forms.mapping(
    firstName ->
      text()
        .verifying(emptyError(firstName), isNonEmpty)
        .verifying(lengthError(firstName), isNotExceedingMaxLength(_, 20))
        .verifying(nonAlphabeticError(firstName),
                   containsOnlyAlphaAndWhitespacesAnd(_, allowedChars)
        ),
    lastName ->
      text()
        .verifying(emptyError(lastName), isNonEmpty)
        .verifying(lengthError(lastName), isNotExceedingMaxLength(_, 20))
        .verifying(nonAlphabeticError(lastName),
                   containsOnlyAlphaAndWhitespacesAnd(_, allowedChars)
        )
  )(FullName.apply)(FullName.unapply)

  def form(): Form[FullName] = Form(mapping)

  private def emptyError(field: String) = s"primaryContactDetails.fullNamePage.${field}.error.empty"

  private def lengthError(field: String) =
    s"primaryContactDetails.fullNamePage.${field}.error.length"

  private def nonAlphabeticError(field: String) =
    s"primaryContactDetails.fullNamePage.${field}.error.specialCharacters"

}
