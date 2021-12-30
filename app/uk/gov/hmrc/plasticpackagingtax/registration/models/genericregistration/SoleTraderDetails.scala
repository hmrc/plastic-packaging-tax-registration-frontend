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
  nino: Option[String],
  sautr: Option[String],
  trn: Option[String],
  override val identifiersMatch: Boolean,
  override val businessVerification: Option[GrsBusinessVerification],
  override val registration: GrsRegistration
) extends GrsResponse

object GrsSoleTraderDetails {
  implicit val format: Format[GrsSoleTraderDetails] = Json.format[GrsSoleTraderDetails]
}

case class SoleTraderDetails(
  firstName: String,
  lastName: String,
  dateOfBirth: Option[String],
  ninoOrTrn: String,
  sautr: Option[String],
  override val registration: Option[RegistrationDetails]
) extends HasRegistrationDetails

object SoleTraderDetails {

  implicit val format: Format[SoleTraderDetails] =
    Json.format[SoleTraderDetails]

  def apply(grsSoleTraderDetails: GrsSoleTraderDetails): SoleTraderDetails =
    SoleTraderDetails(grsSoleTraderDetails.fullName.firstName,
                      grsSoleTraderDetails.fullName.lastName,
                      Some(grsSoleTraderDetails.dateOfBirth),
                      grsSoleTraderDetails.nino.getOrElse(
                        grsSoleTraderDetails.trn.getOrElse(
                          throw new IllegalStateException("Nino or Trn is required")
                        )
                      ),
                      grsSoleTraderDetails.sautr,
                      Some(
                        RegistrationDetails(
                          identifiersMatch = grsSoleTraderDetails.identifiersMatch,
                          verificationStatus =
                            grsSoleTraderDetails.businessVerification.map { bv =>
                              bv.verificationStatus
                            },
                          registrationStatus =
                            grsSoleTraderDetails.registration.registrationStatus,
                          registeredBusinessPartnerId =
                            grsSoleTraderDetails.registration.registeredBusinessPartnerId
                        )
                      )
    )

}
