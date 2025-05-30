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

import play.api.libs.json.{Json, OFormat}

case class CreateEmailVerificationRequest(
  credId: String,
  continueUrl: String,
  origin: String,
  accessibilityStatementUrl: String,
  email: Email,
  backUrl: String,
  pageTitle: String,
  deskproServiceName: String,
  lang: String = "en"
)

object CreateEmailVerificationRequest {

  implicit val format: OFormat[CreateEmailVerificationRequest] =
    Json.format[CreateEmailVerificationRequest]

}

case class Email(address: String, enterUrl: String)

object Email {
  implicit val format: OFormat[Email] = Json.format[Email]
}
