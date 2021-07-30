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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{await, redirectLocation, status}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class IncorpIdControllerSpec extends ControllerSpec {

  private val mcc          = stubMessagesControllerComponents()
  private val registration = aRegistration()

  private val controller =
    new IncorpIdController(authenticate = mockAuthAction,
                           mockJourneyAction,
                           mockRegistrationConnector,
                           mockIncorpIdConnector,
                           mockSoleTraderConnector,
                           mockGeneralPartnershipConnector,
                           mockScottishPartnershipConnector,
                           mcc
    )(ec)

  private val unregisteredLimitedCompany = aRegistration(
    withOrganisationDetails(unregisteredUkOrgDetails(OrgType.UK_COMPANY))
  )

  private val registeredLimitedCompany = aRegistration(
    withOrganisationDetails(registeredUkOrgDetails(OrgType.UK_COMPANY))
  )

  private val unregisteredSoleTrader = aRegistration(
    withOrganisationDetails(unregisteredUkOrgDetails(OrgType.SOLE_TRADER))
  )

  private val registeredSoleTrader = aRegistration(
    withOrganisationDetails(registeredUkOrgDetails(OrgType.SOLE_TRADER))
  )

  private val unregisteredGeneralPartnership = aRegistration(
    withOrganisationDetails(unregisteredPartnershipDetails(GENERAL_PARTNERSHIP))
  )

  private val unregisteredScottishPartnership = aRegistration(
    withOrganisationDetails(unregisteredPartnershipDetails(SCOTTISH_PARTNERSHIP))
  )

  private val registeredGeneralPartnership = aRegistration(
    withOrganisationDetails(registeredGeneralPartnershipDetails())
  )

  private val registeredScottishPartnership = aRegistration(
    withOrganisationDetails(registeredScottishPartnershipDetails())
  )

  "incorpIdCallback" should {

    "redirect to the registration page" when {
      "registering a UK Limited Company" in {
        val result = simulateLimitedCompanyCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
      }

      "registering a Sole Trader" in {
        val result = simulateSoleTraderCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
        getRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a GeneralPartnership" in {
        val result = simulateGeneralPartnershipCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
        getRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }

      "registering a ScottishPartnership" in {
        val result = simulateScottishPartnershipCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
        getRegistration.incorpJourneyId mustBe registration.incorpJourneyId
      }
    }

    "store the returned business entity information" when {
      "registering a UK Limited Company" in {
        await(simulateLimitedCompanyCallback())

        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockRegistrationConnector).update(registrationCaptor.capture())(any[HeaderCarrier]())

        registrationCaptor.getValue.organisationDetails.incorporationDetails mustBe Some(
          incorporationDetails
        )
        registrationCaptor.getValue.organisationDetails.soleTraderDetails mustBe None
        registrationCaptor.getValue.organisationDetails.partnershipDetails mustBe None
      }

      "registering a Sole Trader" in {
        await(simulateSoleTraderCallback())

        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockRegistrationConnector).update(registrationCaptor.capture())(any[HeaderCarrier]())

        registrationCaptor.getValue.organisationDetails.soleTraderDetails mustBe Some(
          soleTraderIncorporationDetails
        )
        registrationCaptor.getValue.organisationDetails.incorporationDetails mustBe None
        registrationCaptor.getValue.organisationDetails.partnershipDetails mustBe None
      }

      "registering a GeneralPartnership" in {
        await(simulateGeneralPartnershipCallback())

        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockRegistrationConnector).update(registrationCaptor.capture())(any[HeaderCarrier]())
        registrationCaptor.getValue.organisationDetails.partnershipDetails mustBe Some(
          partnershipDetails
        )
        registrationCaptor.getValue.organisationDetails.incorporationDetails mustBe None
        registrationCaptor.getValue.organisationDetails.soleTraderDetails mustBe None
      }

      "registering a ScottishPartnership" in {
        await(simulateScottishPartnershipCallback())

        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockRegistrationConnector).update(registrationCaptor.capture())(any[HeaderCarrier]())
        registrationCaptor.getValue.organisationDetails.partnershipDetails mustBe Some(
          partnershipDetailsWithScottishPartnership
        )
        registrationCaptor.getValue.organisationDetails.incorporationDetails mustBe None
        registrationCaptor.getValue.organisationDetails.soleTraderDetails mustBe None
      }
    }

    "throw exception" when {
      "organisation type is invalid" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(withOrganisationDetails(OrganisationDetails(isBasedInUk = Some(true))))
        )

        intercept[InternalServerException] {
          await(controller.incorpIdCallback("uuid-id")(getRequest()))
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
        mockRegistrationUpdate(registeredGeneralPartnership)

        intercept[IllegalStateException] {
          await(controller.incorpIdCallback("uuid-id")(getRequest()))
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
        mockRegistrationUpdate(registeredGeneralPartnership)

        intercept[IllegalStateException] {
          await(controller.incorpIdCallback("uuid-id")(getRequest()))
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
        mockRegistrationUpdate(registeredScottishPartnership)

        intercept[IllegalStateException] {
          await(controller.incorpIdCallback("uuid-id")(getRequest()))
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
        mockRegistrationUpdate(registeredScottishPartnership)

        intercept[IllegalStateException] {
          await(controller.incorpIdCallback("uuid-id")(getRequest()))
        }
      }

      "DB registration update fails" in {
        authorizedUser()
        mockRegistrationFind(registration)
        mockGetUkCompanyDetails(incorporationDetails)
        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError] {
          await(controller.incorpIdCallback("uuid-id")(getRequest()))
        }
      }

      "GRS call fails" in {
        authorizedUser()
        mockRegistrationFind(registration)
        mockGetUkCompanyDetailsFailure(new RuntimeException("error"))

        intercept[RuntimeException] {
          await(controller.incorpIdCallback("uuid-id")(getRequest()))
        }
      }
    }
  }

  private def simulateLimitedCompanyCallback() = {
    authorizedUser()
    mockGetUkCompanyDetails(incorporationDetails)
    mockRegistrationFind(unregisteredLimitedCompany)
    mockRegistrationUpdate(registeredLimitedCompany)

    controller.incorpIdCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateSoleTraderCallback() = {
    authorizedUser()
    mockGetSoleTraderDetails(soleTraderIncorporationDetails)
    mockRegistrationFind(unregisteredSoleTrader)
    mockRegistrationUpdate(registeredSoleTrader)

    controller.incorpIdCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateGeneralPartnershipCallback() = {
    authorizedUser()
    mockGetGeneralPartnershipDetails(generalPartnershipDetails)
    mockRegistrationFind(unregisteredGeneralPartnership)
    mockRegistrationUpdate(registeredGeneralPartnership)

    controller.incorpIdCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def simulateScottishPartnershipCallback() = {
    authorizedUser()
    mockGetScottishPartnershipDetails(scottishPartnershipDetails)
    mockRegistrationFind(unregisteredScottishPartnership)
    mockRegistrationUpdate(registeredScottishPartnership)

    controller.incorpIdCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def getRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector, Mockito.atLeastOnce()).update(captor.capture())(any())
    captor.getValue
  }

}
