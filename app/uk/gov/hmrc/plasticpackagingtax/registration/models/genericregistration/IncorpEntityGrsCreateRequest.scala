/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Regime.{PPT, Regime}

case class IncorpEntityGrsCreateRequest(
  continueUrl: String,
  optServiceName: Option[String] = None,
  deskProServiceId: String,
  signOutUrl: String,
  accessibilityUrl: String,
  regime: Regime = PPT,
  businessVerificationCheck: Boolean = true
) extends GrsJourneyCreationRequest[IncorpEntityGrsCreateRequest] {

  override def setBusinessVerificationCheckFalse: IncorpEntityGrsCreateRequest =
    copy(businessVerificationCheck = false)

}

object IncorpEntityGrsCreateRequest {

  implicit val format: OFormat[IncorpEntityGrsCreateRequest] =
    Json.format[IncorpEntityGrsCreateRequest]

}
