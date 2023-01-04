/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.libs.json.{Json, OFormat}
import forms.CommonFormValidators

case class JobTitle(value: String)

object JobTitle extends CommonFormValidators {
  implicit val format: OFormat[JobTitle] = Json.format[JobTitle]
  lazy val jobTitleEmptyError            = "primaryContactDetails.jobTitle.empty.error"
  lazy val jobTitleTooLongError          = "primaryContactDetails.jobTitle.tooLong.error"
  lazy val jobTitleNonAlphaCharError     = "primaryContactDetails.jobTitle.nonAlphaChar.error"
  val maxLength                          = 155
  val jobTitle                           = "value"

  def form(): Form[JobTitle] =
    Form(
      mapping(
        jobTitle -> text()
          .verifying(jobTitleEmptyError, isNonEmpty)
          .verifying(jobTitleTooLongError, isNotExceedingMaxLength(_, maxLength))
          .verifying(jobTitleNonAlphaCharError, containsOnlyAlphaAndWhitespacesAnd(_, None))
      )(JobTitle.apply)(JobTitle.unapply)
    )

}
