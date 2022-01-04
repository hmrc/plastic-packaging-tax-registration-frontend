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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  OrgType,
  PARTNERSHIP,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  PartnershipTypeEnum,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration._
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.{
  NOT_SUBSCRIBED,
  SUBSCRIBED,
  Status,
  UNKNOWN
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
      "in progress when not registered" in {
        createOrg(UK_COMPANY, None, false).status mustBe TaskStatus.InProgress
      }
      "in progress when subscription check not done" in {
        createOrg(UK_COMPANY,
                  None,
                  true,
                  subscriptionStatus = None
        ).status mustBe TaskStatus.InProgress
      }
      "in progress when subscribed" in {
        createOrg(UK_COMPANY,
                  None,
                  true,
                  subscriptionStatus = Some(SUBSCRIBED)
        ).status mustBe TaskStatus.InProgress
      }
      "completed when registered and subscription status not-subscribed" in {
        createOrg(UK_COMPANY,
                  None,
                  true,
                  subscriptionStatus = Some(NOT_SUBSCRIBED)
        ).status mustBe TaskStatus.Completed
      }
      "completed when registered and subscription status unknown" in {
        createOrg(UK_COMPANY,
                  None,
                  true,
                  subscriptionStatus = Some(UNKNOWN)
        ).status mustBe TaskStatus.Completed
      }
    }

    "identify that business partner id is present" in {
      forAll(registeredOrgs) { organisationDetails =>
        organisationDetails.businessPartnerId mustBe Some("XP001")
      }
    }

    "identify that business partner id is absent" in {
      forAll(unregisteredOrgs) { organisationDetails =>
        organisationDetails.businessPartnerId mustBe None
      }
    }

    "suggest business partner id absent" when {
      "organisation type is unsupported" in {
        OrganisationDetails(organisationType =
          Some(CHARITABLE_INCORPORATED_ORGANISATION)
        ).businessPartnerId mustBe None
      }

      "partnership type is unsupported" in {
        OrganisationDetails(organisationType = Some(PARTNERSHIP),
                            partnershipDetails = Some(
                              PartnershipDetails(partnershipType = LIMITED_LIABILITY_PARTNERSHIP)
                            )
        ).businessPartnerId mustBe None
      }
    }

    "report business name" when {
      "incorporated business" in {
        val corp = createOrg(UK_COMPANY, None, true)
        corp.businessName mustBe Some(corp.incorporationDetails.get.companyName)
      }

      "sole trader" in {
        val soleTrader        = createOrg(SOLE_TRADER, None, true)
        val soleTraderDetails = soleTrader.soleTraderDetails.get
        soleTrader.businessName mustBe Some(
          s"${soleTraderDetails.firstName} ${soleTraderDetails.lastName}"
        )
      }
    }
  }

  private def createOrg(
    orgType: OrgType,
    partnershipType: Option[PartnershipTypeEnum],
    registered: Boolean,
    subscriptionStatus: Option[Status] = Some(NOT_SUBSCRIBED)
  ) =
    orgType match {
      case UK_COMPANY =>
        OrganisationDetails(organisationType = Some(UK_COMPANY),
                            incorporationDetails = Some(
                              IncorporationDetails(companyNumber = "123",
                                                   companyName = "Test",
                                                   ctutr = "ABC",
                                                   companyAddress = IncorporationAddressDetails(),
                                                   registration =
                                                     if (registered)
                                                       Some(
                                                         RegistrationDetails(
                                                           identifiersMatch = true,
                                                           verificationStatus =
                                                             Some("PASS"),
                                                           registrationStatus = "REGISTERED",
                                                           registeredBusinessPartnerId =
                                                             Some("XP001")
                                                         )
                                                       )
                                                     else
                                                       Some(
                                                         RegistrationDetails(
                                                           identifiersMatch = true,
                                                           verificationStatus =
                                                             Some("FAIL"),
                                                           registrationStatus =
                                                             "REGISTRATION_NOT_CALLED",
                                                           registeredBusinessPartnerId = None
                                                         )
                                                       )
                              )
                            ),
                            subscriptionStatus = subscriptionStatus
        )
      case SOLE_TRADER =>
        OrganisationDetails(organisationType = Some(SOLE_TRADER),
                            soleTraderDetails = Some(
                              SoleTraderDetails(firstName = "Alan",
                                                lastName = "Johnson",
                                                dateOfBirth = Some("12/12/1960"),
                                                ninoOrTrn = "ABC123",
                                                sautr = Some("12345678"),
                                                registration =
                                                  if (registered)
                                                    Some(
                                                      RegistrationDetails(
                                                        identifiersMatch = true,
                                                        verificationStatus =
                                                          Some("PASS"),
                                                        registrationStatus =
                                                          "REGISTERED",
                                                        registeredBusinessPartnerId =
                                                          Some("XP001")
                                                      )
                                                    )
                                                  else
                                                    Some(
                                                      RegistrationDetails(
                                                        identifiersMatch = true,
                                                        verificationStatus =
                                                          Some("FAIL"),
                                                        registrationStatus =
                                                          "REGISTRATION_NOT_CALLED",
                                                        registeredBusinessPartnerId =
                                                          None
                                                      )
                                                    )
                              )
                            ),
                            subscriptionStatus = subscriptionStatus
        )
      case PARTNERSHIP =>
        partnershipType match {
          case Some(GENERAL_PARTNERSHIP) =>
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails = Some(
                                  PartnershipDetails(partnershipType = GENERAL_PARTNERSHIP,
                                                     partnershipName =
                                                       Some("Big Bad General Partners"),
                                                     partnershipBusinessDetails = Some(
                                                       PartnershipBusinessDetails(
                                                         sautr = "12345678",
                                                         postcode =
                                                           "BD19 3BD",
                                                         registration =
                                                           if (registered)
                                                             Some(
                                                               RegistrationDetails(
                                                                 identifiersMatch =
                                                                   true,
                                                                 verificationStatus =
                                                                   Some("PASS"),
                                                                 registrationStatus =
                                                                   "REGISTERED",
                                                                 registeredBusinessPartnerId =
                                                                   Some("XP001")
                                                               )
                                                             )
                                                           else
                                                             Some(
                                                               RegistrationDetails(
                                                                 identifiersMatch =
                                                                   true,
                                                                 verificationStatus =
                                                                   Some("FAIL"),
                                                                 registrationStatus =
                                                                   "REGISTRATION_NOT_CALLED",
                                                                 registeredBusinessPartnerId =
                                                                   None
                                                               )
                                                             ),
                                                         companyProfile = None
                                                       )
                                                     )
                                  )
                                ),
                                subscriptionStatus = subscriptionStatus
            )
          case Some(SCOTTISH_PARTNERSHIP) =>
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails = Some(
                                  PartnershipDetails(partnershipType = SCOTTISH_PARTNERSHIP,
                                                     partnershipName =
                                                       Some("Big Bad Scottish Partners"),
                                                     partnershipBusinessDetails = Some(
                                                       PartnershipBusinessDetails(
                                                         sautr = "12345678",
                                                         postcode = "BD19 3BD",
                                                         registration =
                                                           if (registered)
                                                             Some(
                                                               RegistrationDetails(
                                                                 identifiersMatch = true,
                                                                 verificationStatus =
                                                                   Some("PASS"),
                                                                 registrationStatus = "REGISTERED",
                                                                 registeredBusinessPartnerId =
                                                                   Some("XP001")
                                                               )
                                                             )
                                                           else
                                                             Some(
                                                               RegistrationDetails(
                                                                 identifiersMatch = true,
                                                                 verificationStatus =
                                                                   Some("FAIL"),
                                                                 registrationStatus =
                                                                   "REGISTRATION_NOT_CALLED",
                                                                 registeredBusinessPartnerId = None
                                                               )
                                                             ),
                                                         companyProfile = None
                                                       )
                                                     )
                                  )
                                ),
                                subscriptionStatus = subscriptionStatus
            )
          case _ => fail("Unsupported partnership type")
        }
      case _ => fail("Unsupported organisation type")
    }

  "should serialise to json as expected" in {
    println(Json.toJson(createOrg(UK_COMPANY, None, false)).toString)
  }
}
