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

import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.libs.json.{Format, Reads, Writes}
import forms.CommonFormValidators
import forms.liability.RegType.RegType

object RegType extends Enumeration {
  type RegType = Value

  val GROUP: Value         = Value("Group")
  val SINGLE_ENTITY: Value = Value("SingleEntity")

  def withNameOpt(name: String): Option[Value] = values.find(_.toString == name)

  implicit val format: Format[RegType] = Format(Reads.enumNameReads(RegType), Writes.enumNameWrites)
}

case class RegistrationType(value: Option[RegType])

object RegistrationType extends CommonFormValidators {
  val emptyError = "registrationType.type.empty.error"

  def form(): Form[RegistrationType] =
    Form(
      mapping(
        "value" -> optional(text())
          .verifying(emptyError,_.isDefined)
          .transform[String](_.get,Some.apply)
          .verifying(emptyError, contains(RegType.values.toSeq.map(_.toString)))
      )(RegistrationType.apply)(RegistrationType.unapply)
    )

  def apply(value: String): RegistrationType = RegistrationType(RegType.withNameOpt(value))

  def unapply(registrationType: RegistrationType): Option[String] =
    registrationType.value.map(_.toString)

}
