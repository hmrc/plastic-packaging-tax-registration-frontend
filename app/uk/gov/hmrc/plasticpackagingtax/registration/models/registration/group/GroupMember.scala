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
import java.util.UUID

import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address

case class GroupMember(
  id: String = UUID.randomUUID().toString,
  customerIdentification1: String,
  customerIdentification2: Option[String] = None,
  organisationDetails: Option[OrganisationDetails] = None,
  addressDetails: Address
) {

  lazy val businessName: String =
    organisationDetails.map(_.organisationName).getOrElse("No business name")

  override def equals(o: Any): Boolean =
    o match {
      case o: GroupMember => o.customerIdentification1 == this.customerIdentification1
      case _              => false
    }

}

object GroupMember {
  implicit val format: OFormat[GroupMember] = Json.format[GroupMember]
}
