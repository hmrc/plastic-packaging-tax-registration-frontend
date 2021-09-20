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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  CHARITY_OR_NOT_FOR_PROFIT,
  OrgType,
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  PartnershipTypeEnum,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  GeneralPartnershipDetails,
  IncorporationAddressDetails,
  IncorporationDetails,
  IncorporationRegistrationDetails,
  PartnershipDetails,
  ScottishPartnershipDetails,
  SoleTraderIncorporationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus

class OrganisationDetailsSpec extends AnyWordSpec with Matchers with TableDrivenPropertyChecks {

  val registeredOrgs = Table("Organisation Details",
                             createOrg(UK_COMPANY, None, true),
                             createOrg(SOLE_TRADER, None, true),
                             createOrg(PARTNERSHIP, Some(GENERAL_PARTNERSHIP), true),
                             createOrg(PARTNERSHIP, Some(SCOTTISH_PARTNERSHIP), true)
  )

  val unregisteredOrgs = Table("Organisation Details",
                               createOrg(UK_COMPANY, None, false),
                               createOrg(SOLE_TRADER, None, false),
                               createOrg(PARTNERSHIP, Some(GENERAL_PARTNERSHIP), false),
                               createOrg(PARTNERSHIP, Some(SCOTTISH_PARTNERSHIP), false)
  )

  "OrganisationDetails" should {

    "report task status as expected" when {
      "not started" in {
        OrganisationDetails().status mustBe TaskStatus.NotStarted
      }
      "in progress" in {
        createOrg(UK_COMPANY, None, false).status mustBe TaskStatus.InProgress
      }
      "completed" in {
        createOrg(UK_COMPANY, None, true).status mustBe TaskStatus.Completed
      }
    }

    "identify that business partner id is present" in {
      forAll(registeredOrgs) { organisationDetails =>
        organisationDetails.businessPartnerIdPresent() mustBe true
      }
    }

    "identify that business partner id is absent" in {
      forAll(unregisteredOrgs) { organisationDetails =>
        organisationDetails.businessPartnerIdPresent() mustBe false
      }
    }

    "suggest business partner id absent" when {
      "organisation type is unsupported" in {
        OrganisationDetails(organisationType =
          Some(CHARITY_OR_NOT_FOR_PROFIT)
        ).businessPartnerIdPresent() mustBe false
      }

      "partnership type is unsupported" in {
        OrganisationDetails(organisationType = Some(PARTNERSHIP),
                            partnershipDetails = Some(
                              PartnershipDetails(partnershipType = LIMITED_LIABILITY_PARTNERSHIP)
                            )
        ).businessPartnerIdPresent() mustBe false
      }
    }
  }

  private def createOrg(
    orgType: OrgType,
    partnershipType: Option[PartnershipTypeEnum],
    registered: Boolean
  ) =
    orgType match {
      case UK_COMPANY =>
        OrganisationDetails(isBasedInUk = Some(true),
                            organisationType = Some(UK_COMPANY),
                            incorporationDetails = Some(
                              IncorporationDetails(companyNumber = "123",
                                                   companyName = "Test",
                                                   ctutr = "ABC",
                                                   businessVerificationStatus = "PASS",
                                                   companyAddress = IncorporationAddressDetails(),
                                                   registration =
                                                     if (registered)
                                                       IncorporationRegistrationDetails(
                                                         registrationStatus = "REGISTERED",
                                                         registeredBusinessPartnerId = Some("XP001")
                                                       )
                                                     else
                                                       IncorporationRegistrationDetails(
                                                         registrationStatus =
                                                           "REGISTRATION_NOT_CALLED",
                                                         registeredBusinessPartnerId = None
                                                       )
                              )
                            )
        )
      case SOLE_TRADER =>
        OrganisationDetails(organisationType = Some(SOLE_TRADER),
                            soleTraderDetails = Some(
                              SoleTraderIncorporationDetails(firstName = "Alan",
                                                             lastName = "Johnson",
                                                             dateOfBirth = "12/12/1960",
                                                             nino = "ABC123",
                                                             sautr = Some("12345678"),
                                                             registration =
                                                               if (registered)
                                                                 IncorporationRegistrationDetails(
                                                                   registrationStatus =
                                                                     "REGISTERED",
                                                                   registeredBusinessPartnerId =
                                                                     Some("XP001")
                                                                 )
                                                               else
                                                                 IncorporationRegistrationDetails(
                                                                   registrationStatus =
                                                                     "REGISTRATION_NOT_CALLED",
                                                                   registeredBusinessPartnerId =
                                                                     None
                                                                 )
                              )
                            )
        )
      case PARTNERSHIP =>
        partnershipType match {
          case Some(GENERAL_PARTNERSHIP) =>
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails = Some(
                                  PartnershipDetails(partnershipType = GENERAL_PARTNERSHIP,
                                                     partnershipName =
                                                       Some("Big Bad General Partners"),
                                                     generalPartnershipDetails = Some(
                                                       GeneralPartnershipDetails(sautr = "12345678",
                                                                                 postcode =
                                                                                   "BD19 3BD",
                                                                                 registration = if (
                                                                                   registered
                                                                                 )
                                                                                   IncorporationRegistrationDetails(
                                                                                     registrationStatus =
                                                                                       "REGISTERED",
                                                                                     registeredBusinessPartnerId =
                                                                                       Some("XP001")
                                                                                   )
                                                                                 else
                                                                                   IncorporationRegistrationDetails(
                                                                                     registrationStatus = "REGISTRATION_NOT_CALLED",
                                                                                     registeredBusinessPartnerId =
                                                                                       None
                                                                                   )
                                                       )
                                                     ),
                                                     scottishPartnershipDetails = None
                                  )
                                )
            )
          case Some(SCOTTISH_PARTNERSHIP) =>
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails = Some(
                                  PartnershipDetails(partnershipType = SCOTTISH_PARTNERSHIP,
                                                     partnershipName =
                                                       Some("Big Bad Scottish Partners"),
                                                     generalPartnershipDetails = None,
                                                     scottishPartnershipDetails = Some(
                                                       ScottishPartnershipDetails(
                                                         sautr = "12345678",
                                                         postcode = "BD19 3BD",
                                                         registration =
                                                           if (registered)
                                                             IncorporationRegistrationDetails(
                                                               registrationStatus = "REGISTERED",
                                                               registeredBusinessPartnerId =
                                                                 Some("XP001")
                                                             )
                                                           else
                                                             IncorporationRegistrationDetails(
                                                               registrationStatus =
                                                                 "REGISTRATION_NOT_CALLED",
                                                               registeredBusinessPartnerId = None
                                                             )
                                                       )
                                                     )
                                  )
                                )
            )
          case _ => fail("Unsupported partnership type")
        }
      case _ => fail("Unsupported organisation type")
    }

}
