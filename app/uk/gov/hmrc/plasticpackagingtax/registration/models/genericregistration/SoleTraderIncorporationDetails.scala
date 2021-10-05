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

import play.api.libs.json._

case class GrsFullname(firstName: String, lastName: String)

object GrsFullname {
  implicit val format: Format[GrsFullname] = Json.format[GrsFullname]
}

case class GrsSoleTraderDetails(
  fullName: GrsFullname,
  dateOfBirth: String,
  nino: String,
  sautr: Option[String],
  registration: IncorporationRegistrationDetails
)

object GrsSoleTraderDetails {
  implicit val format: Format[GrsSoleTraderDetails] = Json.format[GrsSoleTraderDetails]
}

case class SoleTraderIncorporationDetails(
  firstName: String,
  lastName: String,
  dateOfBirth: String,
  nino: String,
  sautr: Option[String],
  override val registration: IncorporationRegistrationDetails
) extends RegistrationDetails

object SoleTraderIncorporationDetails {

  implicit val format: Format[SoleTraderIncorporationDetails] =
    Json.format[SoleTraderIncorporationDetails]

  def apply(grsSoleTraderDetails: GrsSoleTraderDetails): SoleTraderIncorporationDetails =
    SoleTraderIncorporationDetails(grsSoleTraderDetails.fullName.firstName,
                                   grsSoleTraderDetails.fullName.lastName,
                                   grsSoleTraderDetails.dateOfBirth,
                                   grsSoleTraderDetails.nino,
                                   grsSoleTraderDetails.sautr,
                                   grsSoleTraderDetails.registration
    )

}
