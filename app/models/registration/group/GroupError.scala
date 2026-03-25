/*
 * Copyright 2026 HM Revenue & Customs
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

package models.registration.group

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Json, OFormat, Reads, Writes}
import models.registration.group.GroupErrorType.given_Format_GroupErrorType

import scala.util.Try

enum GroupErrorType(val value: String):
  case MEMBER_IN_GROUP              extends GroupErrorType("MEMBER_IN_GROUP")
  case MEMBER_IS_NOMINATED          extends GroupErrorType("MEMBER_IS_NOMINATED")
  case MEMBER_IS_ALREADY_REGISTERED extends GroupErrorType("MEMBER_IS_ALREADY_REGISTERED")

object GroupErrorType {
//  implicit def value(errorType: GroupErrorType): String = errorType.toString

  given Format[GroupErrorType] =
    Format(
      Reads {
        case JsString(value) =>
          Try(GroupErrorType.valueOf(value))
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Unknown GroupErrorType: $value"))
        case _ =>
          JsError("String value expected")
      },
      Writes(groupErrorType => JsString(groupErrorType.toString))
    )
}

case class GroupError(errorType: GroupErrorType, memberName: String)

object GroupError {
  implicit val format: OFormat[GroupError] = Json.format[GroupError]
}
