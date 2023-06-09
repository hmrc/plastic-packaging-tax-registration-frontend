/*
 * Copyright 2023 HM Revenue & Customs
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

package models.registration

import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import spec.PptTestData
import forms.organisation.OrgType.{CHARITABLE_INCORPORATED_ORGANISATION, OVERSEAS_COMPANY_UK_BRANCH, PARTNERSHIP, REGISTERED_SOCIETY, SOLE_TRADER, UK_COMPANY}
import forms.organisation.PartnerTypeEnum.{GENERAL_PARTNERSHIP, LIMITED_LIABILITY_PARTNERSHIP, PartnerTypeEnum, SCOTTISH_LIMITED_PARTNERSHIP, SCOTTISH_PARTNERSHIP}
import models.genericregistration._
import models.subscriptions.SubscriptionStatus.{NOT_SUBSCRIBED, SUBSCRIBED, Status, UNKNOWN}
import utils.AddressConversionUtils
import views.viewmodels.TaskStatus

import javax.inject.Inject

class OrganisationDetailsSpec @Inject() (acUtils: AddressConversionUtils)
    extends AnyWordSpec with PptTestData with Matchers with TableDrivenPropertyChecks {

  override val addressConversionUtils: AddressConversionUtils = acUtils

  val registeredOrgs = Table(
    "Organisation Details",
    createRegisteredUkCompanyOrg(),
    createdRegisteredSoleTradeOrg(),
    createRegisteredPartnership(GENERAL_PARTNERSHIP),
    createRegisteredPartnership(SCOTTISH_PARTNERSHIP),
    createRegisteredPartnership(SCOTTISH_LIMITED_PARTNERSHIP)
  )

  val unregisteredOrgs = Table(
    "Organisation Details",
    createFailedUkCompanyOrg(),
    createdFailedSoleTradeOrg(),
    createFailedPartnership(GENERAL_PARTNERSHIP),
    createFailedPartnership(SCOTTISH_PARTNERSHIP),
    createFailedPartnership(SCOTTISH_LIMITED_PARTNERSHIP)
  )

  "OrganisationDetails" should {

    "report task status as expected" when {
      "not started" in {
        OrganisationDetails().status mustBe TaskStatus.NotStarted
      }
      "in progress when not registered" in {
        createFailedUkCompanyOrg(None).status mustBe TaskStatus.InProgress
      }

      "in progress when subscribed" in {
        createRegisteredUkCompanyOrg(Some(SUBSCRIBED)).status mustBe TaskStatus.InProgress
      }
      "completed when registered and subscription status not-subscribed" in {
        createRegisteredUkCompanyOrg(Some(NOT_SUBSCRIBED)).status mustBe TaskStatus.Completed
      }
      "completed when registered and subscription status unknown" in {
        createRegisteredUkCompanyOrg(Some(UNKNOWN)).status mustBe TaskStatus.Completed
      }
    }

    "identify that business partner id is present" in {
      forAll(registeredOrgs) { organisationDetails =>
        organisationDetails.businessPartnerId mustBe Some("XP001")
      }
    }

    "identify that partner business partner id is present for existing partner" in {
      aRegistration(withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))).organisationDetails.partnerGrsRegistration(
        Some("123")
      ) mustBe Some(RegistrationDetails(true, Some("Verified"), "REGISTERED", Some("XM654321")))
    }

    "identify that partner business verification failed " in {
      val partnerWithVerificationFailed = aSoleTraderPartner().copy(soleTraderDetails =
        Some(
          soleTraderDetails.copy(registration =
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
        )
      )
      aRegistration(
        withPartnershipDetails(
          Some(
            generalPartnershipDetailsWithPartners.copy(partners =
              Seq(partnerWithVerificationFailed)
            )
          )
        )
      ).organisationDetails.partnerBusinessVerificationFailed(Some("123")) mustBe true
    }

    "identify that partner business partner verification status" in {
      aRegistration(withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))).organisationDetails.partnerVerificationStatus(
        Some("123")
      ) mustBe Some("Verified")
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
        OrganisationDetails(
          organisationType = Some(PARTNERSHIP),
          partnershipDetails = Some(PartnershipDetails(partnershipType = LIMITED_LIABILITY_PARTNERSHIP))
        ).businessPartnerId mustBe None
      }
    }

    "report business name" when {
      "incorporated business" in {
        val corp = createRegisteredUkCompanyOrg()
        corp.businessName mustBe Some(corp.incorporationDetails.get.companyName)
      }

      "sole trader" in {
        val soleTrader        = createdRegisteredSoleTradeOrg()
        val soleTraderDetails = soleTrader.soleTraderDetails.get
        soleTrader.businessName mustBe Some(s"${soleTraderDetails.firstName} ${soleTraderDetails.lastName}")
      }
    }
    "check if business address is returned from Grs" in {
      forAll(registeredOrgs) { organisationDetails =>
        val updatedOrganisationDetails =
          organisationDetails.withBusinessRegisteredAddress(addressConversionUtils)
        updatedOrganisationDetails.organisationType match {
          case Some(UK_COMPANY) | Some(REGISTERED_SOCIETY) | Some(OVERSEAS_COMPANY_UK_BRANCH) =>
            updatedOrganisationDetails.isBusinessAddressFromGrs mustBe Some(true)
          case _ => updatedOrganisationDetails.isBusinessAddressFromGrs mustBe Some(false)
        }
      }
    }
  }

  private def createdRegisteredSoleTradeOrg(subscriptionStatus: Option[Status] = Some(NOT_SUBSCRIBED)): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(SOLE_TRADER),
      soleTraderDetails = Some(
        SoleTraderDetails(
          firstName = "Alan",
          lastName = "Johnson",
          dateOfBirth = Some("12/12/1960"),
          ninoOrTrn = "ABC123",
          sautr = Some("12345678"),
          registration = Some(createRegistrationDetails(Some("PASS"), "REGISTERED", Some("XP001")))
        )
      ),
      subscriptionStatus = subscriptionStatus
    )

  private def createRegistrationDetails(status: Option[String], registrationStatus: String, partnerId: Option[String]) =
    RegistrationDetails(
      identifiersMatch = true,
      verificationStatus = status,
      registrationStatus = registrationStatus,
      registeredBusinessPartnerId = partnerId
    )

  private def createdFailedSoleTradeOrg(subscriptionStatus: Option[Status] = Some(NOT_SUBSCRIBED)): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(SOLE_TRADER),
      soleTraderDetails = Some(
        SoleTraderDetails(
          firstName = "Alan",
          lastName = "Johnson",
          dateOfBirth = Some("12/12/1960"),
          ninoOrTrn = "ABC123",
          sautr = Some("12345678"),
          registration = Some(createRegistrationDetails(Some("FAIL"), "REGISTRATION_NOT_CALLED", None))
        )
      ),
      subscriptionStatus = subscriptionStatus
    )

  private def createRegisteredUkCompanyOrg(subscriptionStatus: Option[Status] = Some(NOT_SUBSCRIBED)): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(UK_COMPANY),
      incorporationDetails = Some(
        IncorporationDetails(
          companyNumber = "123",
          companyName = "Test",
          ctutr = Some("ABC"),
          companyAddress = testCompanyAddress,
          registration = Some(createRegistrationDetails(Some("PASS"), "REGISTERED", Some("XP001")))
        )
      ),
      subscriptionStatus = subscriptionStatus
    )

  private def createFailedUkCompanyOrg(subscriptionStatus: Option[Status] = Some(NOT_SUBSCRIBED)): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(UK_COMPANY),
      incorporationDetails = Some(
        IncorporationDetails(
          companyNumber = "123",
          companyName = "Test",
          ctutr = Some("ABC"),
          companyAddress = testCompanyAddress,
          registration = Some(createRegistrationDetails(Some("FAIL"), "REGISTRATION_NOT_CALLED", None))
        )
      ),
      subscriptionStatus = subscriptionStatus
    )

  private def createRegisteredPartnership(
    partnershipType: PartnerTypeEnum,
    subscriptionStatus: Option[Status] = Some(NOT_SUBSCRIBED)
  ): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(PARTNERSHIP),
      partnershipDetails = Some(
        PartnershipDetails(
          partnershipType = partnershipType,
          partnershipName = Some(partnershipType.toString),
          partnershipBusinessDetails = Some(
            PartnershipBusinessDetails(
              sautr = "12345678",
              postcode = "BD19 3BD",
              registration = Some(createRegistrationDetails(Some("PASS"), "REGISTERED", Some("XP001"))),
              companyProfile = None
            )
          )
        )
      ),
      subscriptionStatus = subscriptionStatus
    )

  private def createFailedPartnership(
    partnershipType: PartnerTypeEnum,
    subscriptionStatus: Option[Status] = Some(NOT_SUBSCRIBED)
  ): OrganisationDetails =
    OrganisationDetails(
      organisationType = Some(PARTNERSHIP),
      partnershipDetails = Some(
        PartnershipDetails(
          partnershipType = partnershipType,
          partnershipName =
            Some(s"Big Bad ${partnershipType.toString}"),
          partnershipBusinessDetails = Some(
            PartnershipBusinessDetails(
              sautr = "12345678",
              postcode = "BD19 3BD",
              registration = Some(createRegistrationDetails(Some("FAIL"), "REGISTRATION_NOT_CALLED", None)),
              companyProfile = None
            )
          )
        )
      ),
      subscriptionStatus = subscriptionStatus
    )

}
