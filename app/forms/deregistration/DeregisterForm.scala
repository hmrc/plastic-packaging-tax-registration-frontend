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

package forms.deregistration

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import forms.{CommonFormValidators, CommonFormValues}

object DeregisterForm extends CommonFormValidators with CommonFormValues {

  val emptyError = "deregistration.deregister.empty.error"
  val field      = "deregister"

  def form(): Form[Boolean] =
    Form(
      mapping(
        field -> optional(text)
          .verifying(emptyError, _.nonEmpty)
          .transform[String](_.get, Some.apply)
          .transform[Boolean](_ == YES, _.toString)
      )(identity)(Some.apply)
    )

}
