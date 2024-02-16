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

package models.emailverification

import play.api.libs.json._

case class VerificationStatus(emails: Seq[EmailStatus]) {
  def toVerificationStatus: VerificationStatus = VerificationStatus(emails = this.emails)
}

case class EmailStatus(emailAddress: String, verified: Boolean, locked: Boolean)

object EmailStatus {

  implicit val format: Format[EmailStatus] =
    Json.format[EmailStatus]

}

object VerificationStatus {
  implicit val format: Format[VerificationStatus] = Json.format[VerificationStatus]
}
