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

package models.genericregistration

import play.api.libs.json.{Json, OFormat}
import models.genericregistration.Regime.{PPT, Regime}

case class PartnershipGrsCreateRequest(
  continueUrl: String,
  optServiceName: Option[String] = None,
  deskProServiceId: String,
  signOutUrl: String,
  accessibilityUrl: String,
  regime: Regime = PPT,
  enableSautrCheck: Boolean = true,
  businessVerificationCheck: Boolean = true
) extends GrsJourneyCreationRequest[PartnershipGrsCreateRequest] {

  override def setBusinessVerificationCheckFalse: PartnershipGrsCreateRequest =
    copy(businessVerificationCheck = false)

}

object PartnershipGrsCreateRequest {

  implicit val format: OFormat[PartnershipGrsCreateRequest] =
    Json.format[PartnershipGrsCreateRequest]

}
