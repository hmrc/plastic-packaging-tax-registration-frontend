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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{await, redirectLocation, status}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.{
  NOT_SUBSCRIBED,
  SUBSCRIBED
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatusResponse
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class GrsControllerSpec extends ControllerSpec {

  private val mcc          = stubMessagesControllerComponents()
  private val registration = aRegistration()

  private val controller =
    new GrsController(authenticate = mockAuthAction,
                      mockJourneyAction,
                      mockRegistrationConnector,
                      mockUkCompanyGrsConnector,
                      mockSoleTraderGrsConnector,
                      mockRegisteredSocietyGrsConnector,
                      mockGeneralPartnershipGrsConnector,
                      mockScottishPartnershipGrsConnector,
                      mockSubscriptionsConnector,
                      mcc
    )(ec)

  private val unregisteredLimitedCompany = aRegistration(
    withOrganisationDetails(unregisteredUkCompanyOrgDetails())
  )

  private val verificationFailedLimitedCompany = aRegistration(
    withOrganisationDetails(verificationFailedUkCompanyOrgDetails())
  )

  private val registeredLimitedCompany = aRegistration(
    withOrganisationDetails(registeredUkCompanyOrgDetails())
  )

  private val registeredRegisteredSociety = aRegistration(
    withOrganisationDetails(registeredRegisteredSocietyOrgDetails())
  )

  private val unregisteredSoleTrader = aRegistration(
    withOrganisationDetails(unregisteredSoleTraderOrgDetails())
  )

  private val unregisteredGeneralPartnership = aRegistration(
    withOrganisationDetails(unregisteredPartnershipDetails(GENERAL_PARTNERSHIP))
  )

  private val unregisteredScottishPartnership = aRegistration(
    withOrganisationDetails(unregisteredPartnershipDetails(SCOTTISH_PARTNERSHIP))
  )

  "incorpIdCallback" should {

    mockGetSubscriptionStatus(SubscriptionStatusResponse(NOT_SUBSCRIBED, None))

    "redirect to the registration page" when {
      "registering a UK Limited Company" in {
        val result = simulateLimitedCompanyCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.RegistrationController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a Sole Trader" in {
        val result = simulateSoleTraderCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.RegistrationController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a Registered Society" in {
        val result = simulateRegisteredSocietyCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.RegistrationController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a GeneralPartnership" in {
        val result = simulateGeneralPartnershipCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.RegistrationController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a ScottishPartnership" in {
        val result = simulateScottishPartnershipCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.RegistrationController.displayPage().url)
        getLastSavedRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }
    }

    "store the returned business entity information" when {
      "registering a UK Limited Company" in {
        await(simulateLimitedCompanyCallback())

        val organisationDetails = getLastSavedRegistration.organisationDetails

        organisationDetails.incorporationDetails mustBe Some(incorporationDetails)
        organisationDetails.soleTraderDetails mustBe None
        organisationDetails.partnershipDetails mustBe None

        organisationDetails.subscriptionStatus mustBe Some(NOT_SUBSCRIBED)
      }

      "registering a Sole Trader" in {
        await(simulateSoleTraderCallback())

        val organisationDetails = getLastSavedRegistration.organisationDetails

        organisationDetails.soleTraderDetails mustBe Some(soleTraderIncorporationDetails)
        organisationDetails.incorporationDetails mustBe None
        organisationDetails.partnershipDetails mustBe None

        organisationDetails.subscriptionStatus mustBe Some(NOT_SUBSCRIBED)
      }

      "registering a GeneralPartnership" in {
        await(simulateGeneralPartnershipCallback())

        val organisationDetails = getLastSavedRegistration.organisationDetails

        organisationDetails.partnershipDetails mustBe Some(partnershipDetails)
        organisationDetails.incorporationDetails mustBe None
        organisationDetails.soleTraderDetails mustBe None

        organisationDetails.subscriptionStatus mustBe Some(NOT_SUBSCRIBED)
      }

      "registering a ScottishPartnership" in {
        await(simulateScottishPartnershipCallback())

        val organisationDetails = getLastSavedRegistration.organisationDetails

        organisationDetails.partnershipDetails mustBe Some(
          partnershipDetailsWithScottishPartnership
        )
        organisationDetails.incorporationDetails mustBe None
        organisationDetails.soleTraderDetails mustBe None

        organisationDetails.subscriptionStatus mustBe Some(NOT_SUBSCRIBED)
      }
    }

    "throw exception" when {
      "organisation type is invalid" in {
        authorizedUser()
        mockRegistrationFind(aRegistration(withOrganisationDetails(OrganisationDetails())))

        intercept[InternalServerException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "generalPartnership details are missing" in {
        authorizedUser()
        mockGetGeneralPartnershipDetails(generalPartnershipDetails)
        mockRegistrationFind(
          unregisteredGeneralPartnership.copy(organisationDetails =
            unregisteredGeneralPartnership.organisationDetails.copy(partnershipDetails = None)
          )
        )
        mockRegistrationUpdate()

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "unsupported generalPartnership type" in {
        authorizedUser()
        mockGetGeneralPartnershipDetails(generalPartnershipDetails)
        mockRegistrationFind(
          unregisteredGeneralPartnership.copy(organisationDetails =
            unregisteredGeneralPartnership.organisationDetails.copy(partnershipDetails =
              Some(
                unregisteredGeneralPartnership.organisationDetails.partnershipDetails.get.copy(
                  partnershipType = SCOTTISH_LIMITED_PARTNERSHIP
                )
              )
            )
          )
        )
        mockRegistrationUpdate()

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "scottishPartnership details are missing" in {
        authorizedUser()
        mockGetScottishPartnershipDetails(scottishPartnershipDetails)

        mockRegistrationFind(
          unregisteredScottishPartnership.copy(organisationDetails =
            unregisteredScottishPartnership.organisationDetails.copy(partnershipDetails = None)
          )
        )
        mockRegistrationUpdate()

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "unsupported scottishPartnership type" in {
        authorizedUser()
        mockGetScottishPartnershipDetails(scottishPartnershipDetails)
        mockRegistrationFind(
          unregisteredScottishPartnership.copy(organisationDetails =
            unregisteredScottishPartnership.organisationDetails.copy(partnershipDetails =
              Some(
                unregisteredScottishPartnership.organisationDetails.partnershipDetails.get.copy(
                  partnershipType = LIMITED_PARTNERSHIP
                )
              )
            )
          )
        )
        mockRegistrationUpdate()

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "DB registration update fails" in {
        authorizedUser()
        mockRegistrationFind(registration)
        mockGetUkCompanyDetails(incorporationDetails)
        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "GRS call fails" in {
        authorizedUser()
        mockRegistrationFind(registration)
        mockGetUkCompanyDetailsFailure(new RuntimeException("error"))

        intercept[RuntimeException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }
    }

    "show error page" when {
      "business partner id is absent" in {
        authorizedUser()
        val result = simulateUnregisteredLimitedCompanyCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.NotableErrorController.grsFailure().url)
      }
    }

    "show verification error page" when {
      "business verification status is FAIL and registrationStatus is REGISTRATION_NOT_CALLED " in {
        authorizedUser()
        val result = simulateBusinessVerificationFailureLimitedCompanyCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          pptRoutes.NotableErrorController.businessVerificationFailure().url
        )
      }
    }

    "show duplicate subscription page" when {
      "a ppt registration already exists for the organisation" in {
        authorizedUser()
        mockGetUkCompanyDetails(incorporationDetails)
        mockRegistrationFind(registeredLimitedCompany)
        mockRegistrationUpdate()
        mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))

        val result = controller.grsCallback(registration.incorpJourneyId.get)(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          pptRoutes.NotableErrorController.duplicateRegistration().url
        )
      }
    }
  }

  private def simulateLimitedCompanyCallback() = {
    authorizedUser()
    mockGetUkCompanyDetails(incorporationDetails)
    mockRegistrationFind(unregisteredLimitedCompany)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateRegisteredSocietyCallback() = {
    authorizedUser()
    mockGetRegisteredSocietyDetails(incorporationDetails)
    mockRegistrationFind(registeredRegisteredSociety)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateUnregisteredLimitedCompanyCallback() = {
    authorizedUser()
    mockGetUkCompanyDetails(unregisteredIncorporationDetails)
    mockRegistrationFind(unregisteredLimitedCompany)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateBusinessVerificationFailureLimitedCompanyCallback() = {
    authorizedUser()
    mockGetUkCompanyDetails(verificationFailedIncorporationDetails)
    mockRegistrationFind(verificationFailedLimitedCompany)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateSoleTraderCallback() = {
    authorizedUser()
    mockGetSoleTraderDetails(soleTraderIncorporationDetails)
    mockRegistrationFind(unregisteredSoleTrader)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateGeneralPartnershipCallback() = {
    authorizedUser()
    mockGetGeneralPartnershipDetails(generalPartnershipDetails)
    mockRegistrationFind(unregisteredGeneralPartnership)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateScottishPartnershipCallback() = {
    authorizedUser()
    mockGetScottishPartnershipDetails(scottishPartnershipDetails)
    mockRegistrationFind(unregisteredScottishPartnership)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def getLastSavedRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector, Mockito.atLeastOnce()).update(captor.capture())(any())
    captor.getValue
  }

}
