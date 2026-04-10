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

package models.emailverification

import play.api.libs.json
import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}
import models.emailverification.EmailVerificationStatus.given_Format_EmailVerificationStatus
import models.registration.Registration

import scala.util.Try

enum EmailVerificationStatus:
  case VERIFIED, LOCKED_OUT, NOT_VERIFIED

object EmailVerificationStatus {
  given Format[EmailVerificationStatus] =
    Format(
      Reads {
        case JsString(value) =>
          Try(EmailVerificationStatus.valueOf(value))
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Unknown EmailVerificationStatus: $value"))
        case _ =>
          JsError("String value expected")
      },
      Writes(emailVerificationStatus => JsString(emailVerificationStatus.toString))
    )
}

object EmailVerificationStatusMapper {

  def toMap(emailStatuses: Seq[EmailStatus]): Map[String, EmailVerificationStatus] =
    emailStatuses.map(email => email.emailAddress -> toStatus(email)).toMap

  private def toStatus(emailStatus: EmailStatus): EmailVerificationStatus =
    (emailStatus.verified, emailStatus.locked) match
      case (true, _)     => EmailVerificationStatus.VERIFIED
      case (false, true) => EmailVerificationStatus.LOCKED_OUT
      case _             => EmailVerificationStatus.NOT_VERIFIED

}

object LocalEmailVerification {
  def getPrimaryEmailStatus(registration: Registration): EmailVerificationStatus =
    registration.primaryContactDetails.email
      .map(email => registration.metaData.getEmailStatus(email))
      .getOrElse(EmailVerificationStatus.NOT_VERIFIED)
}
