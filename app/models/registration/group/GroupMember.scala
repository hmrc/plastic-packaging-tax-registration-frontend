/*
 * Copyright 2024 HM Revenue & Customs
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

package models.registration.group

import play.api.libs.json.{Json, OFormat}
import forms.contact.Address
import forms.organisation.OrgType

import java.util.UUID

case class GroupMember(
  id: String = UUID.randomUUID().toString,
  customerIdentification1: String,
  customerIdentification2: Option[String] = None,
  organisationDetails: Option[OrganisationDetails] = None,
  contactDetails: Option[GroupMemberContactDetails] = None,
  addressDetails: Address,
  regWithoutIDFlag: Option[Boolean] = None
) {

  lazy val businessName: String =
    organisationDetails.map(_.organisationName).getOrElse("No business name")

  lazy val businessType: Option[String] =
    organisationDetails.map(_.organisationType)

  lazy val businessTypeDisplayName: String =
    businessType match {
      case Some(organisationType) =>
        OrgType.withNameOpt(organisationType).getOrElse(throw new IllegalStateException("Organisation type is absent"))
      case None => throw new IllegalStateException("Organisation type is absent")
    }

  override def equals(o: Any): Boolean =
    o match {
      case o: GroupMember => o.customerIdentification1 == this.customerIdentification1
      case _              => false
    }

  def withUpdatedGroupMemberName(firstName: String, lastName: String): GroupMember =
    this.copy(contactDetails = Some(this.contactDetails match {
      case Some(cd) => cd.copy(firstName = firstName, lastName = lastName)
      case None     => GroupMemberContactDetails(firstName = firstName, lastName = lastName)
    }))

  def withUpdatedGroupMemberEmail(email: String): GroupMember =
    withUpdatedContactDetails(cd => cd.copy(email = Some(email)))

  def withUpdatedGroupMemberPhoneNumber(phoneNumber: String): GroupMember =
    withUpdatedContactDetails(cd => cd.copy(phoneNumber = Some(phoneNumber)))

  def withUpdatedGroupMemberAddress(address: Option[Address]): GroupMember =
    withUpdatedContactDetails(cd => cd.copy(address = address))

  def isValid: Boolean =
    customerIdentification1.trim.nonEmpty &&
      organisationDetails.exists(_.organisationName.trim.nonEmpty) &&
      contactDetails.exists(_.isValid) &&
      addressDetails.isValid

  private def withUpdatedContactDetails(update: GroupMemberContactDetails => GroupMemberContactDetails) =
    this.copy(contactDetails = Some(this.contactDetails match {
      case Some(cd) => update(cd)
      case None     => throw new IllegalStateException("No contact details found")
    }))

}

object GroupMember {
  implicit val format: OFormat[GroupMember] = Json.format[GroupMember]
}
