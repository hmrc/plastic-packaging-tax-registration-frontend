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

package forms.liability

import forms.mappings.Mappings
import forms.partner.AddPartner.YES
import play.api.data.Form
import forms.{CommonFormValidators, CommonFormValues}
import play.api.data.Forms.mapping

import javax.inject.Inject

class ExpectToExceedThresholdWeight @Inject() extends Mappings {

  val emptyError = "liability.expectToExceedThresholdWeight.question.empty.error"

//  def apply(): Form[Boolean] =
//    Form("value" -> toBoolean(emptyError)
//      .verifying(emptyError, _.nonEmpty)
//      .transform[String](_.get, Some.apply)
//      .transform[Boolean](_ == YES, _.toString)
//    )
  def apply(): Form[Boolean] =
    Form(
      mapping(
        "value" -> toBoolean(emptyError)
      )(identity)(Some.apply)
    )

}
