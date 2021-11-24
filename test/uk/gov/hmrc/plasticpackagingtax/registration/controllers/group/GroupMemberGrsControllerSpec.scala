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
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{GroupDetail, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.{
  NOT_SUBSCRIBED,
  SUBSCRIBED
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatusResponse
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class GroupMemberGrsControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val controller =
    new GroupMemberGrsController(authenticate = mockAuthAction,
                                 mockJourneyAction,
                                 mockRegistrationConnector,
                                 mockUkCompanyGrsConnector,
                                 mockSubscriptionsConnector,
                                 mcc
    )(ec)

  private def registrationWithSelectedGroupMember(orgType: OrgType) =
    aRegistration(
      withGroupDetail(
        Some(
          GroupDetail(membersUnderGroupControl = Some(true),
                      members = Seq.empty,
                      currentMemberOrganisationType = Some(orgType)
          )
        )
      )
    )

  "group member incorpIdCallback " should {

    mockGetSubscriptionStatus(SubscriptionStatusResponse(NOT_SUBSCRIBED, None))

    "redirect to the group member organisation details type controller page" when {
      "registering a UK Limited Company group member" in {
        val result =
          simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(OrgType.UK_COMPANY))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
      }

      "registering an overseas company with uk branch group member" in {
        val result =
          simulateLimitedCompanyCallback(
            registrationWithSelectedGroupMember(OrgType.OVERSEAS_COMPANY_UK_BRANCH)
          )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
      }
    }

    "store the returned business entity information" when {
      "registering UK Limited Company group member" in {
        await(
          simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(OrgType.UK_COMPANY))
        )

        val memberDetails: GroupMember = getLastSavedRegistration.groupDetail.get.members.last

        memberDetails.organisationDetails.get.organisationType mustBe OrgType.UK_COMPANY.toString
        memberDetails.organisationDetails.get.organisationName mustBe incorporationDetails.companyName
        memberDetails.addressDetails mustBe incorporationDetails.companyAddress.toGroupAddressDetails
      }

      "registering an overseas company with uk branch group member" in {
        await(
          simulateLimitedCompanyCallback(
            registrationWithSelectedGroupMember(OrgType.OVERSEAS_COMPANY_UK_BRANCH)
          )
        )

        val memberDetails: GroupMember = getLastSavedRegistration.groupDetail.get.members.last

        memberDetails.organisationDetails.get.organisationType mustBe OrgType.OVERSEAS_COMPANY_UK_BRANCH.toString
        memberDetails.organisationDetails.get.organisationName mustBe incorporationDetails.companyName
        memberDetails.addressDetails mustBe incorporationDetails.companyAddress.toGroupAddressDetails
      }
    }

    "not store business enity information " when {
      "registering UK Limited Company with same company number" in {
        authorizedUser()
        mockGetUkCompanyDetails(incorporationDetails)
        val registration = registrationWithSelectedGroupMember(OrgType.UK_COMPANY)
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        await(controller.grsCallback(registration.incorpJourneyId.get)(getRequest()))

        val membersListSize: Int = getLastSavedRegistration.groupDetail.get.members.size

        membersListSize mustBe 1
      }

      "registering UK Limited Company with same company number as Nominated organisation" in {
        authorizedUser()
        mockGetUkCompanyDetails(
          IncorporationDetails("12345678",
                               testCompanyName,
                               testUtr,
                               testBusinessVerificationPassStatus,
                               testCompanyAddress,
                               incorporationRegistrationDetails
          )
        )
        val registration = registrationWithSelectedGroupMember(OrgType.UK_COMPANY)
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        await(controller.grsCallback(registration.incorpJourneyId.get)(getRequest()))

        val noGroupMembers: Boolean = getLastSavedRegistration.groupDetail.get.members.isEmpty

        noGroupMembers mustBe true
      }
    }

    "throw exception" when {
      "organisation type is invalid" in {
        authorizedUser()
        mockRegistrationFind(registrationWithSelectedGroupMember(OrgType.TRUST))

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "groupDetail is None" in {
        authorizedUser()
        mockRegistrationFind(aRegistration(withGroupDetail(None)))

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "DB registration update fails" in {
        authorizedUser()
        mockRegistrationFind(registrationWithSelectedGroupMember(OrgType.UK_COMPANY))
        mockGetUkCompanyDetails(incorporationDetails)
        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "GRS call fails" in {
        authorizedUser()
        mockRegistrationFind(registrationWithSelectedGroupMember(OrgType.UK_COMPANY))
        mockGetUkCompanyDetailsFailure(new RuntimeException("error"))

        intercept[RuntimeException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

      "show duplicate subscription page" when {
        "a subscription already exists for the group member" in {
          authorizedUser()
          mockGetUkCompanyDetails(
            IncorporationDetails("123467890",
                                 testCompanyName,
                                 testUtr,
                                 testBusinessVerificationPassStatus,
                                 testCompanyAddress,
                                 incorporationRegistrationDetails
            )
          )
          val registration = registrationWithSelectedGroupMember(OrgType.UK_COMPANY)
          mockRegistrationFind(registration)
          mockRegistrationUpdate()
          mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))

          val result =
            controller.grsCallback(registration.incorpJourneyId.get)(getRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            pptRoutes.NotableErrorController.duplicateRegistration().url
          )
        }
      }
    }
  }

  private def simulateLimitedCompanyCallback(registration: Registration) = {
    authorizedUser()
    mockGetUkCompanyDetails(
      IncorporationDetails("123467890",
                           testCompanyName,
                           testUtr,
                           testBusinessVerificationPassStatus,
                           testCompanyAddress,
                           incorporationRegistrationDetails
      )
    )
    mockRegistrationFind(registration)
    mockRegistrationUpdate()

    controller.grsCallback(registration.incorpJourneyId.get)(getRequest())
  }

  private def getLastSavedRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector, Mockito.atLeastOnce()).update(captor.capture())(any())
    captor.getValue
  }

}
