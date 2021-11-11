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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

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
  GroupDetail,
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class GroupMemberGrsControllerSpec extends ControllerSpec {

  private val mcc          = stubMessagesControllerComponents()
  private val registration = aRegistration()

  private val controller =
    new GroupMemberGrsController(authenticate = mockAuthAction,
                                 mockJourneyAction,
                                 mockRegistrationConnector,
                                 mockUkCompanyGrsConnector,
                                 mcc
    )(ec)

  private val registeredLimitedCompany = aRegistration(
    withGroupDetail(
      Some(
        GroupDetail(membersUnderGroupControl = Some(true),
                    members = Seq.empty,
                    currentMemberOrganisationType = Some(OrgType.UK_COMPANY)
        )
      )
    )
  )

  "group member incorpIdCallback " should {
    "redirect to the group member organisation details type controller page" when {
      "registering a UK Limited Company" in {
        val result = simulateLimitedCompanyCallback()

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.OrganisationDetailsTypeController.displayPage().url
        )
      }

    }

    "store the returned business entity information" when {
      "registering UK Limited Company group member" in {
        await(simulateLimitedCompanyCallback())

        val memberDetails = getLastSavedRegistration.groupDetail.get.members.last

        memberDetails.organisationDetails mustBe groupMemberDetailsUKCompany.organisationDetails
        memberDetails.addressDetails mustBe groupMemberDetailsUKCompany.addressDetails
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

      "DB registration update fails" in {
        authorizedUser()
        mockRegistrationFind(registeredLimitedCompany)
        mockGetUkCompanyDetails(incorporationDetails)
        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "GRS call fails" in {
        authorizedUser()
        mockRegistrationFind(registeredLimitedCompany)
        mockGetUkCompanyDetailsFailure(new RuntimeException("error"))

        intercept[RuntimeException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }
    }
  }

  private def simulateLimitedCompanyCallback() = {
    authorizedUser()
    mockGetUkCompanyDetails(incorporationDetails)
    mockRegistrationFind(registeredLimitedCompany)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def getLastSavedRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector, Mockito.atLeastOnce()).update(captor.capture())(any())
    captor.getValue
  }

}
