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

package uk.gov.hmrc.plasticpackagingtax.registration.forms.group

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{CommonFormValidators, CommonFormValues}

case class AddOrganisation(answer: Option[Boolean])

object AddOrganisation extends CommonFormValidators with CommonFormValues {

  val emptyError = "addOrganisation.empty.error"
  val field      = "addOrganisation"

  def form(): Form[AddOrganisation] =
    Form(
      mapping(
        field -> optional(text)
          .verifying(emptyError, _.nonEmpty)
      )(AddOrganisation.toForm)(AddOrganisation.fromForm)
    )

  def toForm(value: Option[String]): AddOrganisation =
    value match {
      case Some(YES) => AddOrganisation(Some(true))
      case Some(NO)  => AddOrganisation(Some(false))
      case _         => AddOrganisation(None)
    }

  def fromForm(addOrganisation: AddOrganisation): Option[Option[String]] =
    addOrganisation.answer.map(value => if (value) Some(YES) else Some(NO))

}
