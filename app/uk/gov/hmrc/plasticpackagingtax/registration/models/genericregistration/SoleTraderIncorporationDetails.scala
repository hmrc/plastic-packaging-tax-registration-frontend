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

case class SoleTraderIncorporationDetails(
  firstName: String,
  lastName: String,
  dateOfBirth: String,
  nino: String
)

object SoleTraderIncorporationDetails {

  val apiReads: Reads[SoleTraderIncorporationDetails] = (
    (__ \ "firstName").read[String] and
      (__ \ "lastName").read[String] and
      (__ \ "dateOfBirth").read[String] and
      (__ \ "nino").read[String]
  )(SoleTraderIncorporationDetails.apply _)

  val apiWrites: Writes[SoleTraderIncorporationDetails] = (
    (__ \ "firstName").write[String] and
      (__ \ "lastName").write[String] and
      (__ \ "dateOfBirth").write[String] and
      (__ \ "nino").write[String]
  )(unlift(SoleTraderIncorporationDetails.unapply))

  val apiFormat: Format[SoleTraderIncorporationDetails] =
    Format[SoleTraderIncorporationDetails](apiReads, apiWrites)

  implicit val format: Format[SoleTraderIncorporationDetails] =
    Json.format[SoleTraderIncorporationDetails]

}
