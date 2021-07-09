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
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType
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
                           mcc
    )(ec)

  "incorpIdCallback" should {

    "redirect to the registration page" when {
      "registering an Uk Limited company" in {
        authorizedUser()
        mockGetUkCompanyDetails(incorporationDetails)
        mockRegistrationFind(
          aRegistration(withOrganisationDetails(unregisteredUkOrgDetails(OrgType.UK_COMPANY)))
        )
        mockRegistrationUpdate(
          aRegistration(withOrganisationDetails(registeredUkOrgDetails(OrgType.UK_COMPANY)))
        )

        val result = controller.incorpIdCallback(registration.incorpJourneyId.get)(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
      }

      "registering an Uk Sole Trader" in {
        authorizedUser()
        mockGetSoleTraderDetails(soleTraderIncorporationDetails)
        mockRegistrationFind(
          aRegistration(withOrganisationDetails(unregisteredUkOrgDetails(OrgType.SOLE_TRADER)))
        )
        mockRegistrationUpdate(
          aRegistration(withOrganisationDetails(registeredUkOrgDetails(OrgType.SOLE_TRADER)))
        )

        val result = controller.incorpIdCallback(registration.incorpJourneyId.get)(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
        getRegistration.incorpJourneyId mustBe registration.incorpJourneyId
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

      "DB registration update fails" in {
        authorizedUser()
        mockRegistrationFind(registration)
        mockGetUkCompanyDetails(incorporationDetails)
        mockRegistrationFailure()

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

  def getRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector, Mockito.atLeastOnce()).update(captor.capture())(any())
    captor.getValue
  }

}
