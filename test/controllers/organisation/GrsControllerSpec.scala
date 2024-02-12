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

package controllers.organisation

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.verify
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Result
import play.api.test.Helpers.{await, redirectLocation, status}
import uk.gov.hmrc.http.InternalServerException
import connectors.DownstreamServiceError
import controllers.group.{routes => groupRoutes}
import controllers.organisation.{routes => orgRoutes}
import controllers.{routes => pptRoutes}
import forms.liability.RegType
import forms.organisation.PartnerTypeEnum.{GENERAL_PARTNERSHIP, SCOTTISH_PARTNERSHIP}
import models.genericregistration.{IncorporationDetails, PartnershipBusinessDetails, SoleTraderDetails}
import models.registration.{OrganisationDetails, Registration}
import models.subscriptions.SubscriptionStatus.{NOT_SUBSCRIBED, SUBSCRIBED}
import models.subscriptions.SubscriptionStatusResponse
import play.api.test.FakeRequest
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class GrsControllerSpec extends ControllerSpec {

  private val mcc          = stubMessagesControllerComponents()
  private val registration = aRegistration()

  private val controller =
    new GrsController(
      journeyAction = spyJourneyAction,
      mockRegistrationConnector,
      mockUkCompanyGrsConnector,
      mockSoleTraderGrsConnector,
      mockRegisteredSocietyGrsConnector,
      mockPartnershipGrsConnector,
      mockSubscriptionsConnector,
      addressConversionUtils,
      mcc
    )(ec)

  private val unregisteredLimitedCompany = aRegistration(withOrganisationDetails(unregisteredUkCompanyOrgDetails()))

  private val verificationFailedLimitedCompany: Registration = aRegistration(withOrganisationDetails(verificationFailedUkCompanyOrgDetails()))

  private val verificationFailedSoleTrader: Registration =
    aRegistration(withSoleTraderDetails(Some(verificationFailedSoleTraderDetails)))

  private val registeredLimitedCompany = aRegistration(withOrganisationDetails(registeredUkCompanyOrgDetails()))

  private val registeredRegisteredSociety = aRegistration(withOrganisationDetails(registeredRegisteredSocietyOrgDetails()))

  private val unregisteredSoleTrader = aRegistration(withOrganisationDetails(unregisteredSoleTraderOrgDetails()))

  private val unregisteredGeneralPartnership = aRegistration(
    withOrganisationDetails(unregisteredPartnershipDetails(GENERAL_PARTNERSHIP, Some("General Partnership")))
  )

  private val unregisteredScottishPartnership = aRegistration(
    withOrganisationDetails(unregisteredPartnershipDetails(SCOTTISH_PARTNERSHIP, Some("Scottish Partnership")))
  )

  "GRS Callback" should {

    mockGetSubscriptionStatus(SubscriptionStatusResponse(NOT_SUBSCRIBED, None))

    "redirect to the task list page" when {
      "registering a UK Limited Company" in {
        val result = simulateLimitedCompanyCallback(incorporationDetails)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(orgRoutes.ConfirmBusinessAddressController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a Sole Trader" in {
        val result = simulateSoleTraderCallback(soleTraderDetails)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(orgRoutes.ConfirmBusinessAddressController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a Registered Society" in {
        val result = simulateRegisteredSocietyCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(orgRoutes.ConfirmBusinessAddressController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a GeneralPartnership" in {
        val result = simulateGeneralPartnershipCallback(partnershipBusinessDetails)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(orgRoutes.ConfirmBusinessAddressController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a ScottishPartnership" in {
        val result = simulateScottishPartnershipCallback(partnershipBusinessDetails)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(orgRoutes.ConfirmBusinessAddressController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }
    }

    "store the returned business entity information" when {
      "registering a UK Limited Company" in {
        await(simulateLimitedCompanyCallback(incorporationDetails))

        val organisationDetails = getLastSavedRegistration.organisationDetails

        organisationDetails.incorporationDetails mustBe Some(incorporationDetails)
        organisationDetails.soleTraderDetails mustBe None
        organisationDetails.partnershipDetails mustBe None
        organisationDetails.businessRegisteredAddress mustBe Some(addressConversionUtils
          .toPptAddress(incorporationDetails.companyAddress))
        organisationDetails.subscriptionStatus mustBe Some(NOT_SUBSCRIBED)
      }

      "registering a Sole Trader" in {
        await(simulateSoleTraderCallback(soleTraderDetails))

        val organisationDetails = getLastSavedRegistration.organisationDetails

        organisationDetails.soleTraderDetails mustBe Some(soleTraderDetails)
        organisationDetails.incorporationDetails mustBe None
        organisationDetails.partnershipDetails mustBe None

        organisationDetails.subscriptionStatus mustBe Some(NOT_SUBSCRIBED)
      }

      "registering a GeneralPartnership" in {
        await(simulateGeneralPartnershipCallback(partnershipBusinessDetails))

        val organisationDetails = getLastSavedRegistration.organisationDetails

        organisationDetails.partnershipDetails mustBe Some(generalPartnershipDetails)
        organisationDetails.incorporationDetails mustBe None
        organisationDetails.soleTraderDetails mustBe None

        organisationDetails.subscriptionStatus mustBe Some(NOT_SUBSCRIBED)
      }

      "registering a ScottishPartnership" in {
        await(simulateScottishPartnershipCallback(partnershipBusinessDetails))

        val organisationDetails = getLastSavedRegistration.organisationDetails

        organisationDetails.partnershipDetails mustBe Some(scottishPartnershipDetails)
        organisationDetails.incorporationDetails mustBe None
        organisationDetails.soleTraderDetails mustBe None

        organisationDetails.subscriptionStatus mustBe Some(NOT_SUBSCRIBED)
      }
    }

    "throw exception" when {
      "organisation type is invalid" in {

        spyJourneyAction.setReg(aRegistration(withOrganisationDetails(OrganisationDetails())))

        intercept[InternalServerException] {
          await(controller.grsCallback("uuid-id")(FakeRequest()))
        }
      }

      "generalPartnership details are missing" in {

        mockGetPartnershipBusinessDetails(partnershipBusinessDetails)
        spyJourneyAction.setReg(
          unregisteredGeneralPartnership.copy(organisationDetails =
            unregisteredGeneralPartnership.organisationDetails.copy(partnershipDetails = None)
          )
        )
        mockRegistrationUpdate()

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(FakeRequest()))
        }
      }

      "scottishPartnership details are missing" in {

        mockGetPartnershipBusinessDetails(partnershipBusinessDetails)

        spyJourneyAction.setReg(
          unregisteredScottishPartnership.copy(organisationDetails =
            unregisteredScottishPartnership.organisationDetails.copy(partnershipDetails = None)
          )
        )
        mockRegistrationUpdate()

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(FakeRequest()))
        }
      }

      "DB registration update fails" in {

        spyJourneyAction.setReg(registration)
        mockGetUkCompanyDetails(incorporationDetails)
        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError] {
          await(controller.grsCallback("uuid-id")(FakeRequest()))
        }
      }

      "GRS call fails" in {

        spyJourneyAction.setReg(registration)
        mockGetUkCompanyDetailsFailure(new RuntimeException("error"))

        intercept[RuntimeException] {
          await(controller.grsCallback("uuid-id")(FakeRequest()))
        }
      }
    }

    "show error page" when {
      "business partner id is absent" in {

        mockGetUkCompanyDetails(unregisteredIncorporationDetails)
        spyJourneyAction.setReg(unregisteredLimitedCompany)
        mockRegistrationUpdate()
    
        val result: Future[Result] = controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())

        val exception              = intercept[Exception](await(result))
        exception.getMessage must include("some-journey-id")
      }
    }

    "show verification error page" when {
      "business verification status is FAIL and registrationStatus is REGISTRATION_NOT_CALLED" in {

        val result = simulateBusinessVerificationFailureLimitedCompanyCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.NotableErrorController.businessVerificationFailure().url)
      }

      "sole trader business verification status is FAIL and registrationStatus is REGISTRATION_NOT_CALLED" in {

        mockGetSoleTraderDetails(verificationFailedSoleTraderDetails)
        spyJourneyAction.setReg(verificationFailedSoleTrader)
        mockRegistrationUpdate()

        val result = controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.NotableErrorController.soleTraderVerificationFailure().url)
      }
    }

    "show duplicate subscription page" when {
      "a ppt registration already exists for the organisation" in {

        mockGetUkCompanyDetails(incorporationDetails)
        spyJourneyAction.setReg(registeredLimitedCompany)
        mockRegistrationUpdate()
        mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))

        val result = controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.NotableErrorController.duplicateRegistration().url)
      }
    }

    "show nominated organisation already registered page" when {
      "a ppt registration already exists for the organisation" in {

        mockGetUkCompanyDetails(incorporationDetails)
        val registeredCompanyForGroup =
          registeredLimitedCompany.copy(registrationType = Some(RegType.GROUP))
        spyJourneyAction.setReg(registeredCompanyForGroup)
        mockRegistrationUpdate()
        mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))

        val result = controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(groupRoutes.NotableErrorController.nominatedOrganisationAlreadyRegistered().url)
      }
    }

    "show registration failed page" when {
      "the registration fails and the identifiers dont match" in {
        val grsResponse = incorporationDetails.copy(registration = Some(failedRegistrationDetails))

        mockGetUkCompanyDetails(grsResponse)
        spyJourneyAction.setReg(registeredLimitedCompany)
        mockRegistrationUpdate()

        val result = controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must include(
          controllers.routes.NotableErrorController.registrationFailed("").url
        )
      }
    }
  }

  private def simulateLimitedCompanyCallback(incorporationDetails: IncorporationDetails) = {

    mockGetUkCompanyDetails(incorporationDetails)
    spyJourneyAction.setReg(unregisteredLimitedCompany)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())
  }

  private def simulateRegisteredSocietyCallback() = {

    mockGetRegisteredSocietyDetails(incorporationDetails)
    spyJourneyAction.setReg(registeredRegisteredSociety)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())
  }


  private def simulateBusinessVerificationFailureLimitedCompanyCallback() = {

    mockGetUkCompanyDetails(verificationFailedIncorporationDetails)
    spyJourneyAction.setReg(verificationFailedLimitedCompany)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())
  }

  private def simulateSoleTraderCallback(soleTraderDetails: SoleTraderDetails) = {

    mockGetSoleTraderDetails(soleTraderDetails)
    spyJourneyAction.setReg(unregisteredSoleTrader)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())
  }

  private def simulateGeneralPartnershipCallback(partnershipBusinessDetails: PartnershipBusinessDetails) = {

    mockGetPartnershipBusinessDetails(partnershipBusinessDetails)
    spyJourneyAction.setReg(unregisteredGeneralPartnership)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())
  }

  private def simulateScottishPartnershipCallback(partnershipBusinessDetails: PartnershipBusinessDetails) = {

    mockGetPartnershipBusinessDetails(partnershipBusinessDetails)
    spyJourneyAction.setReg(unregisteredScottishPartnership)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(FakeRequest())
  }

  private def getLastSavedRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector, Mockito.atLeastOnce()).update(captor.capture())(any())
    captor.getValue
  }

}
