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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address

case class PartnerContactDetails(
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  emailAddress: Option[String] = None,
  phoneNumber: Option[String] = None,
  address: Option[Address] = None
) {

  lazy val name: Option[String] =
    if (firstName.isDefined && lastName.isDefined) Some(s"${firstName.get} ${lastName.get}")
    else None

  def withUpdatedAddress(updatedAddress: Address): PartnerContactDetails =
    this.copy(address = Some(updatedAddress))

}

object PartnerContactDetails {
  implicit val format: OFormat[PartnerContactDetails] = Json.format[PartnerContactDetails]
}
