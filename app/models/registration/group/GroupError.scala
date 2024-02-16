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

package models.registration.group

import play.api.libs.json.{Format, Json, OFormat, Reads, Writes}
import models.registration.group.GroupErrorType.GroupErrorType

object GroupErrorType extends Enumeration {
  type GroupErrorType = Value
  val MEMBER_IN_GROUP: Value              = Value("MEMBER_IN_GROUP")
  val MEMBER_IS_NOMINATED: Value          = Value("MEMBER_IS_NOMINATED")
  val MEMBER_IS_ALREADY_REGISTERED: Value = Value("MEMBER_IS_ALREADY_REGISTERED")

  implicit def value(errorType: GroupErrorType): String = errorType.toString

  implicit val format: Format[GroupErrorType] =
    Format(Reads.enumNameReads(GroupErrorType), Writes.enumNameWrites)

}

case class GroupError(errorType: GroupErrorType, memberName: String)

object GroupError {
  implicit val format: OFormat[GroupError] = Json.format[GroupError]
}
