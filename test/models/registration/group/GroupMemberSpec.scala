/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.play.PlaySpec
import forms.contact.Address
import forms.contact.Address.UKAddress

class GroupMemberSpec extends PlaySpec with TableDrivenPropertyChecks {

  val groupMember: GroupMember =
    GroupMember(
      customerIdentification1 = "",
      addressDetails =
        UKAddress(addressLine1 = "", addressLine2 = None, addressLine3 = None, townOrCity = "", postCode = "")
    )

  val inputData = Table(
    (
      "label",
      "identification1",
      "organisationName",
      "firstName",
      "lastName",
      "phoneNumber",
      "emailAddress",
      "addressLine1",
      "addressLine2",
      "CountryCode"
    ),
    (
      "customerIdentification1 is invalid",
      " ",
      "anyOrgName",
      "Jon",
      "Sparrow",
      "12456",
      "test:test.com",
      "address1",
      "address2",
      "countryCode"
    ),
    (
      "organisationName is invalid",
      "anyId",
      "",
      "Jon",
      "Sparrow",
      "12456",
      "test:test.com",
      "address1",
      "address2",
      "countryCode"
    ),
    (
      "firstName is invalid",
      "anyId",
      "anyOrgName",
      "  ",
      "Sparrow",
      "12456",
      "test:test.com",
      "address1",
      "address2",
      "countryCode"
    ),
    (
      "lastName is invalid",
      "anyId",
      "anyOrgName",
      "Jon",
      "",
      "12456",
      "test:test.com",
      "address1",
      "address2",
      "countryCode"
    ),
    (
      "phoneNumber is invalid",
      "anyId",
      "anyOrgName",
      "Jon",
      "Sparrow",
      " ",
      "test:test.com",
      "address1",
      "address2",
      "countryCode"
    ),
    (
      "emailAddress is invalid",
      "anyId",
      "anyOrgName",
      "Jon",
      "Sparrow",
      "12456",
      "",
      "address1",
      "address2",
      "countryCode"
    ),
    (
      "countryCode is invalid",
      "anyId",
      "anyOrgName",
      "Jon",
      "Sparrow",
      "12456",
      "test:test.com",
      "address1",
      "address2",
      ""
    )
  )

  "isValid" must {
    "return false" when {

      forAll(inputData) {
        (
          label,
          identification1,
          organisationName,
          firstName,
          lastName,
          phoneNumber,
          emailAddress,
          addressLine1,
          addressLine2,
          countryCode
        ) =>
          s"$label" in {

            val sut = createGroupMember(
              identification1,
              organisationName,
              firstName,
              lastName,
              phoneNumber,
              emailAddress,
              addressLine1,
              addressLine2,
              countryCode
            )

            sut.isValid mustBe false
          }
      }
    }
    "return true" when {

      "the group member is valid" in {
        val sut = createGroupMember(
          "identification1",
          "organisationName",
          "firstName",
          "lastName",
          "phoneNumber",
          "emailAddress",
          "addressLine1",
          "addressLine2",
          "countryCode"
        )

        sut.isValid mustBe true
      }

    }
  }

  private def createGroupMember(
    identification: String,
    organisationName: String,
    firstName: String,
    lastName: String,
    phoneNumber: String,
    emailAddress: String,
    addressLine1: String,
    addressLine2: String,
    countryCode: String
  ) =
    GroupMember(
      customerIdentification1 = identification,
      organisationDetails = Some(OrganisationDetails("any", organisationName, None)),
      contactDetails = Some(
        GroupMemberContactDetails(
          firstName = firstName,
          lastName = lastName,
          phoneNumber = Some(phoneNumber),
          email = Some(emailAddress)
        )
      ),
      addressDetails = Address(
        addressLine1 = addressLine1,
        addressLine2 = Some(addressLine2),
        addressLine3 = None,
        countryCode = countryCode,
        townOrCity = "London",
        maybePostcode = Some("NG67JK")
      )
    )

}
