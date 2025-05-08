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

package forms.enrolment

import play.api.data.Forms.{optional, text}
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}
import forms.{CommonFormValidators, CommonFormValues}

case class IsUkAddress(value: Option[Boolean]) {
  def requiresPostCode: Boolean = value.contains(true)
}

object IsUkAddress extends CommonFormValidators with CommonFormValues {
  implicit val format: OFormat[IsUkAddress] = Json.format[IsUkAddress]

  private val mapping = Forms.mapping(
    "value" -> optional(text)
      .verifying("enrolment.isUkAddress.value.error.empty", _.nonEmpty)
  )(IsUkAddress.toForm)(IsUkAddress.fromForm)

  def toForm(value: Option[String]): IsUkAddress =
    value match {
      case Some(YES) => IsUkAddress(Some(true))
      case Some(NO)  => IsUkAddress(Some(false))
      case _         => IsUkAddress(None)
    }

  def fromForm(ukAddress: IsUkAddress): Option[Option[String]] =
    ukAddress.value.map(isUkAddress => if (isUkAddress) Some(YES) else Some(NO))

  def form(): Form[IsUkAddress] = Form(mapping)
}
