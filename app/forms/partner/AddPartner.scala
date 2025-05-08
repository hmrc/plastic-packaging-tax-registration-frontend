/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.partner

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import forms.{CommonFormValidators, CommonFormValues}

case class AddPartner(answer: Option[Boolean])

object AddPartner extends CommonFormValidators with CommonFormValues {

  val emptyError = "addPartner.empty.error"
  val field      = "addPartner"

  def form(): Form[AddPartner] =
    Form(
      mapping(
        field -> optional(text)
          .verifying(emptyError, _.nonEmpty)
      )(AddPartner.toForm)(AddPartner.fromForm)
    )

  def toForm(value: Option[String]): AddPartner =
    value match {
      case Some(YES) => AddPartner(Some(true))
      case Some(NO)  => AddPartner(Some(false))
      case _         => AddPartner(None)
    }

  def fromForm(addPartner: AddPartner): Option[Option[String]] =
    addPartner.answer.map(value => if (value) Some(YES) else Some(NO))

}
