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
import forms.CommonFormValidators
import forms.partner.AddPartner.{NO, YES}

case class RemovePartner(value: Option[Boolean])

object RemovePartner extends CommonFormValidators {

  def form(): Form[RemovePartner] =
    Form(
      mapping(
        "value" -> optional(text)
          .verifying("partnership.removePartner.error.empty", _.nonEmpty)
      )(RemovePartner.toForm)(RemovePartner.fromForm)
    )

  def toForm(value: Option[String]): RemovePartner =
    value match {
      case Some(YES) => RemovePartner(Some(true))
      case Some(NO)  => RemovePartner(Some(false))
      case _         => RemovePartner(None)
    }

  def fromForm(removePartner: RemovePartner): Option[Option[String]] =
    removePartner.value.map { removePartner =>
      if (removePartner) Some(YES) else Some(NO)
    }

}
