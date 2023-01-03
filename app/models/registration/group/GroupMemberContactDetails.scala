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

package models.registration.group

import play.api.libs.json.{Json, OFormat}
import forms.contact.Address

case class GroupMemberContactDetails(
  firstName: String,
  lastName: String,
  phoneNumber: Option[String] = None,
  email: Option[String] = None,
  address: Option[Address] = None
) {
  val groupMemberName = s"$firstName $lastName"

  def isValid: Boolean =
    firstName.trim.nonEmpty &&
      lastName.trim.nonEmpty &&
      phoneNumber.exists(_.trim.nonEmpty) &&
      email.exists(_.trim.nonEmpty)

}

object GroupMemberContactDetails {
  implicit val format: OFormat[GroupMemberContactDetails] = Json.format[GroupMemberContactDetails]

}
