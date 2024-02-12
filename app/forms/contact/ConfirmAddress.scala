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

package forms.contact

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import forms.{CommonFormValidators, CommonFormValues}

case class ConfirmAddress(useRegisteredAddress: Option[Boolean])

object ConfirmAddress extends CommonFormValidators with CommonFormValues {

  val emptyError = "primaryContactDetails.confirmAddress.empty.error"
  val field      = "useRegisteredAddress"

  def form(): Form[ConfirmAddress] =
    Form(
      mapping(
        field -> optional(text)
          .verifying(emptyError, _.nonEmpty)
      )(ConfirmAddress.toForm)(ConfirmAddress.fromForm)
    )

  def toForm(value: Option[String]): ConfirmAddress =
    value match {
      case Some(YES) => ConfirmAddress(Some(true))
      case Some(NO)  => ConfirmAddress(Some(false))
      case _         => ConfirmAddress(None)
    }

  def fromForm(confirmAddress: ConfirmAddress): Option[Option[String]] =
    confirmAddress.useRegisteredAddress.map(value => if (value) Some(YES) else Some(NO))

}
