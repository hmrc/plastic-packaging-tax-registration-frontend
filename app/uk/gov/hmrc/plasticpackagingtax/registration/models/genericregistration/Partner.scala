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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.PartnerTypeEnum

import java.util.UUID

case class Partner(
  id: String = UUID.randomUUID().toString,
  partnerType: Option[PartnerTypeEnum],
  soleTraderDetails: Option[SoleTraderDetails] = None,
  incorporationDetails: Option[IncorporationDetails] = None,
  partnerPartnershipDetails: Option[PartnerPartnershipDetails] = None,
  contactDetails: Option[PartnerContactDetails] = None
) {

  def isCompleted: Boolean =
    partnerType.nonEmpty && (soleTraderDetails.nonEmpty || incorporationDetails.nonEmpty || partnerPartnershipDetails.nonEmpty)

}

object Partner {
  implicit val format: OFormat[Partner] = Json.format[Partner]
}
