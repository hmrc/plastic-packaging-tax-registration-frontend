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

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json._

case class IncorporationDetails(
  companyNumber: String,
  companyName: String,
  ctutr: String,
  companyAddress: IncorporationAddressDetails,
  registration: IncorporationRegistrationDetails
)

object IncorporationDetails {

  val apiReads: Reads[IncorporationDetails] = (
    (__ \ "companyProfile" \ "companyNumber").read[String] and
      (__ \ "companyProfile" \ "companyName").read[String] and
      (__ \ "ctutr").read[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress").read[IncorporationAddressDetails] and
      (__ \ "registration").read[IncorporationRegistrationDetails]
  )(IncorporationDetails.apply _)

  val apiWrites: Writes[IncorporationDetails] = (
    (__ \ "companyProfile" \ "companyNumber").write[String] and
      (__ \ "companyProfile" \ "companyName").write[String] and
      (__ \ "ctutr").write[String] and
      (__ \ "companyProfile" \ "unsanitisedCHROAddress").write[IncorporationAddressDetails] and
      (__ \ "registration").write[IncorporationRegistrationDetails]
  )(unlift(IncorporationDetails.unapply))

  val apiFormat: Format[IncorporationDetails] = Format[IncorporationDetails](apiReads, apiWrites)

  implicit val format: Format[IncorporationDetails] = Json.format[IncorporationDetails]
}
