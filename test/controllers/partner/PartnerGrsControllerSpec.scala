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

package controllers.partner

import base.unit.ControllerSpec
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{await, redirectLocation, status}
import uk.gov.hmrc.http.InternalServerException
import controllers.partner.{routes => partnerRoutes}
import controllers.{routes => pptRoutes}
import forms.organisation.PartnerTypeEnum
import forms.organisation.PartnerTypeEnum.{CHARITABLE_INCORPORATED_ORGANISATION, LIMITED_LIABILITY_PARTNERSHIP, LIMITED_PARTNERSHIP, OVERSEAS_COMPANY_NO_UK_BRANCH, OVERSEAS_COMPANY_UK_BRANCH, REGISTERED_SOCIETY, SCOTTISH_LIMITED_PARTNERSHIP, SCOTTISH_PARTNERSHIP, SOLE_TRADER, UK_COMPANY}
import models.genericregistration.PartnerPartnershipDetails
import models.registration.NewRegistrationUpdateService
import models.subscriptions.SubscriptionStatus.{NOT_SUBSCRIBED, SUBSCRIBED}
import models.subscriptions.SubscriptionStatusResponse
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerGrsControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  protected val mockNewRegistrationUpdater = new NewRegistrationUpdateService(mockRegistrationConnector)

  private val controller =
    new PartnerGrsController(
      journeyAction = spyJourneyAction,
      mockUkCompanyGrsConnector,
      mockSoleTraderGrsConnector,
      mockPartnershipGrsConnector,
      mockRegisteredSocietyGrsConnector,
      mockNewRegistrationUpdater,
      mockSubscriptionsConnector,
      mcc
    )(ec)

  "PartnerGrsController " should {
    mockGetSubscriptionStatus(SubscriptionStatusResponse(NOT_SUBSCRIBED, None))
    "redirect from GRS to partner contact name or error page" when {
      forAll(
        Seq(
          (
            SCOTTISH_LIMITED_PARTNERSHIP,
            scottishPartnershipDetails.copy(inflightPartner = Some(nominatedPartner(SCOTTISH_LIMITED_PARTNERSHIP)))
          ),
          (
            LIMITED_LIABILITY_PARTNERSHIP,
            llpPartnershipDetails.copy(inflightPartner = Some(nominatedPartner(LIMITED_LIABILITY_PARTNERSHIP)))
          ),
          (
            SOLE_TRADER,
            scottishPartnershipDetails.copy(inflightPartner = Some(nominatedPartner(PartnerTypeEnum.SOLE_TRADER)))
          ),
          (
            UK_COMPANY,
            scottishPartnershipDetails.copy(inflightPartner = Some(nominatedPartner(PartnerTypeEnum.UK_COMPANY)))
          ),
          (
            REGISTERED_SOCIETY,
            scottishPartnershipDetails.copy(inflightPartner =
              Some(nominatedPartner(PartnerTypeEnum.REGISTERED_SOCIETY))
            )
          ),
          (
            OVERSEAS_COMPANY_UK_BRANCH,
            scottishPartnershipDetails.copy(inflightPartner =
              Some(nominatedPartner(PartnerTypeEnum.OVERSEAS_COMPANY_UK_BRANCH))
            )
          ),
          (
            SCOTTISH_PARTNERSHIP,
            scottishPartnershipDetails.copy(inflightPartner =
              Some(nominatedPartner(PartnerTypeEnum.SCOTTISH_PARTNERSHIP))
            )
          ),
          (
            OVERSEAS_COMPANY_NO_UK_BRANCH,
            scottishPartnershipDetails.copy(inflightPartner =
              Some(nominatedPartner(PartnerTypeEnum.OVERSEAS_COMPANY_NO_UK_BRANCH))
            )
          ),
          (
            CHARITABLE_INCORPORATED_ORGANISATION,
            scottishPartnershipDetails.copy(inflightPartner =
              Some(nominatedPartner(PartnerTypeEnum.CHARITABLE_INCORPORATED_ORGANISATION))
            )
          )
        )
      ) { partnershipDetails =>
        s"${partnershipDetails._1} type was selected" in {
          val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails._2)))

          spyJourneyAction.setReg(registration)
          mockRegistrationUpdate()

          partnershipDetails._1 match {
            case SOLE_TRADER => mockGetSoleTraderDetails(soleTraderDetails)
            case UK_COMPANY | OVERSEAS_COMPANY_UK_BRANCH =>
              mockGetUkCompanyDetails(incorporationDetails)
            case REGISTERED_SOCIETY =>
              mockGetRegisteredSocietyDetails(incorporationDetails)
            case LIMITED_LIABILITY_PARTNERSHIP | LIMITED_PARTNERSHIP | SCOTTISH_PARTNERSHIP |
                SCOTTISH_LIMITED_PARTNERSHIP =>
              mockGetPartnershipBusinessDetails(partnershipBusinessDetails)
            case _ => None
          }

          partnershipDetails._1 match {
            case CHARITABLE_INCORPORATED_ORGANISATION | OVERSEAS_COMPANY_NO_UK_BRANCH =>
              intercept[InternalServerException](
                await(controller.grsCallbackNewPartner(registration.incorpJourneyId.get)(FakeRequest()))
              )
            case _ =>
              val result =
                controller.grsCallbackNewPartner(registration.incorpJourneyId.get)(FakeRequest())
              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(partnerRoutes.PartnerContactNameController.displayNewPartner.url)
          }

        }
      }
    }

    "redirect from GRS to partner contact name or error page" when {
      s"for an existing partner" in {
        val registration =
          aRegistration(withPartnershipDetails(Some(generalPartnershipDetailsWithPartners)))

        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()
        mockGetSoleTraderDetails(soleTraderDetails)
        val result =
          controller.grsCallbackExistingPartner(registration.incorpJourneyId.get, "123")(FakeRequest())
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          partnerRoutes.PartnerContactNameController.displayExistingPartner("123").url
        )
      }
    }

    "show duplicate subscription page" when {
      "a registration already exists for the organisation" in {
        val registration = aRegistration(
          withPartnershipDetails(
            Some(
              scottishPartnershipDetails.copy(inflightPartner = Some(nominatedPartner(PartnerTypeEnum.UK_COMPANY)))
            )
          )
        )

        mockGetUkCompanyDetails(incorporationDetails)
        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()
        mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))

        val result =
          controller.grsCallbackNewPartner(registration.incorpJourneyId.get)(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.NotableErrorController.duplicateRegistration().url)
      }
    }

    "update registration" when {
      "called back to with a supported partner type" in {
        val registration = aRegistration(
          withPartnershipDetails(
            Some(
              scottishPartnershipDetails.copy(inflightPartner =
                Some(nominatedPartner(PartnerTypeEnum.SCOTTISH_PARTNERSHIP))
              )
            )
          )
        )

        mockGetUkCompanyDetails(incorporationDetails)
        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()
        mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))
        mockGetPartnershipBusinessDetails(partnershipBusinessDetails)

        val result =
          controller.grsCallbackNewPartner(registration.incorpJourneyId.get)(FakeRequest())

        status(result) mustBe SEE_OTHER

        // businessPartnerId is an example of a field we would expect to have captured from GRS
        modifiedRegistration.organisationDetails.partnerBusinessPartnerId(None) mustBe Some("XXPPTP123456789")
      }
    }

    "update registration" when {
      "preserve user supplied partner name for partner types which do not have a GRS supplied name" in {
        val partnerWithUserSuppliedName =
          nominatedPartner(PartnerTypeEnum.SCOTTISH_PARTNERSHIP).copy(partnerPartnershipDetails =
            Some(
              PartnerPartnershipDetails().copy(partnershipName = Some("User supplied partnership name"))
            )
          )

        val registration = aRegistration(
          withPartnershipDetails(
            Some(
              generalPartnershipDetails.copy(inflightPartner = Some(partnerWithUserSuppliedName))
            )
          )
        )

        mockGetUkCompanyDetails(incorporationDetails)
        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()
        mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))
        mockGetPartnershipBusinessDetails(partnershipBusinessDetails)

        val result =
          controller.grsCallbackNewPartner(registration.incorpJourneyId.get)(FakeRequest())

        status(result) mustBe SEE_OTHER

        // businessPartnerId is an example of a field we would expect to have captured from GRS
        modifiedRegistration.inflightPartner.flatMap(
          _.partnerPartnershipDetails.flatMap(_.partnershipName)
        ) mustBe Some("User supplied partnership name")
      }
    }

  }

}
