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
import play.api.libs.json.{__, Format, Json, OFormat, Reads, Writes}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.GrsEntityDetails

case class PartnershipDetails(
  sautr: String,
  postcode: String,
  override val registration: IncorporationRegistrationDetails
) extends RegistrationDetails with GrsEntityDetails

object PartnershipDetails {

  val apiReads: Reads[PartnershipDetails] = (
    (__ \ "sautr").read[String] and
      (__ \ "postcode").read[String] and
      (__ \ "registration").read[IncorporationRegistrationDetails]
  )(PartnershipDetails.apply _)

  val apiWrites: Writes[PartnershipDetails] = (
    (__ \ "sautr").write[String] and
      (__ \ "postcode").write[String] and
      (__ \ "registration").write[IncorporationRegistrationDetails]
  )(unlift(PartnershipDetails.unapply))

  val apiFormat: Format[PartnershipDetails] =
    Format[PartnershipDetails](apiReads, apiWrites)

  implicit val format: OFormat[PartnershipDetails] =
    Json.format[PartnershipDetails]

}
