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

package forms.liability

import play.api.data.Forms.{optional, text}
import play.api.data.{Form, Forms}
import play.api.libs.json.{Json, OFormat}
import forms.{CommonFormValidators, CommonFormValues}

case class MembersUnderGroupControl(value: Option[Boolean])

object MembersUnderGroupControl extends CommonFormValidators with CommonFormValues {

  implicit val format: OFormat[MembersUnderGroupControl] = Json.format[MembersUnderGroupControl]

  private val mapping = Forms.mapping(
    "value" -> optional(text)
      .verifying("group.membersUnderGroupControl.error.empty", _.nonEmpty)
  )(MembersUnderGroupControl.toForm)(MembersUnderGroupControl.fromForm)

  def toForm(value: Option[String]): MembersUnderGroupControl =
    value match {
      case Some(YES) => MembersUnderGroupControl(Some(true))
      case Some(NO)  => MembersUnderGroupControl(Some(false))
      case _         => MembersUnderGroupControl(None)
    }

  def fromForm(membersUnderGroupControl: MembersUnderGroupControl): Option[Option[String]] =
    membersUnderGroupControl.value.map(isMembersUnderSameGroupControl =>
      if (isMembersUnderSameGroupControl) Some(YES) else Some(NO)
    )

  def form(): Form[MembersUnderGroupControl] = Form(mapping)
}
