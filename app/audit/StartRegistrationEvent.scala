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

package audit

import play.api.libs.json.{Json, OFormat}
import audit.UserType.UserType
import forms.organisation.OrgType.OrgType

case class StartRegistrationEvent(userType: UserType, internalId: String) {}

object StartRegistrationEvent {
  implicit val format: OFormat[StartRegistrationEvent] = Json.format[StartRegistrationEvent]
  val eventType: String                                = "startPPTRegistration"
}

case class ResumeRegistrationEvent(
  userType: UserType,
  internalId: String,
  orgType: Option[OrgType]
) {}

object ResumeRegistrationEvent {
  implicit val format: OFormat[ResumeRegistrationEvent] = Json.format[ResumeRegistrationEvent]
  val eventType: String                                 = "resumePPTRegistration"
}