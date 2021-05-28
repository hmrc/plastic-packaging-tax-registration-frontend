/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._

case class IncorporationRegistrationDetails(
  registrationStatus: String,
  registeredBusinessPartnerId: String
)

object IncorporationRegistrationDetails {

  val apiReads: Reads[IncorporationRegistrationDetails] = (
    (__ \ "registration" \ "registrationStatus").read[String] and
      (__ \ "registration" \ "registeredBusinessPartnerId").read[String]
  )(IncorporationRegistrationDetails.apply _)

  val apiWrites: Writes[IncorporationRegistrationDetails] = (
    (__ \ "registration" \ "registrationStatus").write[String] and
      (__ \ "registration" \ "registeredBusinessPartnerId").write[String]
  )(unlift(IncorporationRegistrationDetails.unapply))

  val apiFormat: Format[IncorporationRegistrationDetails] =
    Format[IncorporationRegistrationDetails](apiReads, apiWrites)

  implicit val format: OFormat[IncorporationRegistrationDetails] =
    Json.format[IncorporationRegistrationDetails]

}
