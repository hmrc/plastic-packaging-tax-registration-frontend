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

package models.emailverification

import play.api.libs.json
import play.api.libs.json.{Format, Reads, Writes}
import models.emailverification.EmailVerificationStatus.EmailVerificationStatus
import models.registration.Registration

object EmailVerificationStatus extends Enumeration {
  type EmailVerificationStatus = Value
  val VERIFIED: Value     = Value
  val LOCKED_OUT: Value   = Value
  val NOT_VERIFIED: Value = Value

  implicit val format: Format[EmailVerificationStatus] =
    json.Format(Reads.enumNameReads(EmailVerificationStatus), Writes.enumNameWrites)

}

object EmailVerificationStatusMapper {

  def toMap(emailStatuses: Seq[EmailStatus]): Map[String, EmailVerificationStatus] =
    emailStatuses.map(email => email.emailAddress -> toStatus(email)).toMap

  private def toStatus(emailStatus: EmailStatus): EmailVerificationStatus =
    if (emailStatus.verified)
      EmailVerificationStatus.VERIFIED
    else if (emailStatus.locked)
      EmailVerificationStatus.LOCKED_OUT
    else
      EmailVerificationStatus.NOT_VERIFIED

}

object LocalEmailVerification {

  def getPrimaryEmailStatus(registration: Registration): EmailVerificationStatus =
    registration.primaryContactDetails.email.map(email => registration.metaData.getEmailStatus(email)).getOrElse(
      EmailVerificationStatus.NOT_VERIFIED
    )

}
