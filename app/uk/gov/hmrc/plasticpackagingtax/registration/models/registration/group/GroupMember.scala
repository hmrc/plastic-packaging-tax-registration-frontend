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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.OrgType

case class GroupMember(
  customerIdentification1: String,
  customerIdentification2: Option[String],
  organisationDetails: Option[OrganisationDetails],
  addressDetails: AddressDetails
)

object GroupMember {
  implicit val format: OFormat[GroupMember] = Json.format[GroupMember]

  def apply(orgType: OrgType): GroupMember =
    GroupMember(customerIdentification1 = "",
                customerIdentification2 = None,
                organisationDetails =
                  Some(OrganisationDetails(orgType.toString, "")),
                addressDetails = AddressDetails("", "", None, None, None, "")
    )

}
