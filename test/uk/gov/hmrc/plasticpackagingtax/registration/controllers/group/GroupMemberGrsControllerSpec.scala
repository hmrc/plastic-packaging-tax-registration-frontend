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
import org.mockito.Mockito.{reset, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{await, redirectLocation, status}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupErrorType.{
  MEMBER_IN_GROUP,
  MEMBER_IS_NOMINATED
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  GroupError,
  GroupMember,
  OrganisationDetails => GroupOrgDetails
}
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

  private def registrationWithSelectedGroupMember(
    orgType: OrgType,
    existingMembers: Seq[GroupMember] = Seq.empty
  ) =
    aRegistration(
      withGroupDetail(
        Some(
          GroupDetail(membersUnderGroupControl = Some(true),
                      members = existingMembers,
                      currentMemberOrganisationType = Some(orgType)
          )
        )
      )
    )

  private val supportedOrgTypes   = Seq(OrgType.UK_COMPANY, OrgType.OVERSEAS_COMPANY_UK_BRANCH)
  private val unsupportedOrgTypes = OrgType.values.filterNot(supportedOrgTypes.contains)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    mockGetSubscriptionStatus(SubscriptionStatusResponse(NOT_SUBSCRIBED, None))
  }

  override protected def afterEach(): Unit = {
    reset(mockUkCompanyGrsConnector, mockRegistrationConnector, mockSubscriptionsConnector)
    super.afterEach()
  }

  "group member incorpIdCallback " should {

    forAll(supportedOrgTypes) { orgType =>
      "redirect to the group member organisation details type controller page" when {

        "registering a member " + orgType in {
          val result =
            simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(orgType))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
        }
      }

      "store the returned business entity information" when {
        "registering group member " + orgType in {
          await(simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(orgType)))

          groupMemberSize(getLastSavedRegistration) mustBe 1

          val memberDetails: GroupMember = getLastSavedRegistration.groupDetail.get.members.last

          memberDetails.organisationDetails.get.organisationType mustBe orgType.toString
          memberDetails.organisationDetails.get.organisationName mustBe incorporationDetails.companyName
          memberDetails.addressDetails mustBe incorporationDetails.companyAddress.toPptAddress
        }

      }

      "show error page and not store business entity information " when {
        "registering with same company number " + orgType in {
          authorizedUser()
          val detailsFromGrs =
            incorporationDetails.copy(companyNumber = "22222", companyName = "Existing Company")
          mockGetUkCompanyDetails(detailsFromGrs)
          val existingMember = groupMember.copy(
            customerIdentification1 = "22222",
            organisationDetails =
              Some(GroupOrgDetails("UkCompany", "Existing Company", Some(safeNumber)))
          )
          val registration = registrationWithSelectedGroupMember(orgType, Seq(existingMember))
          mockRegistrationFind(registration)
          mockRegistrationUpdate()

          groupMemberSize(registration) mustBe 1

          val result = controller.grsCallback(registration.incorpJourneyId.get)(getRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            routes.NotableErrorController.organisationAlreadyInGroup().url
          )

          groupMemberSize(getLastSavedRegistration) mustBe 1
          groupError(getLastSavedRegistration) mustBe Some(
            GroupError(MEMBER_IN_GROUP, "Existing Company")
          )
        }

        "registering Company with same company number as Nominated organisation " + orgType in {
          authorizedUser()

          val registration = registrationWithSelectedGroupMember(orgType)
          mockRegistrationFind(registration)
          mockRegistrationUpdate()

          groupMemberSize(registration) mustBe 0

          val detailsFromGrs = incorporationDetails.copy(
            companyNumber = registration.organisationDetails.incorporationDetails.get.companyNumber,
            companyName = registration.organisationDetails.incorporationDetails.get.companyName
          )
          mockGetUkCompanyDetails(detailsFromGrs)

          val result = controller.grsCallback(registration.incorpJourneyId.get)(getRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            routes.NotableErrorController.organisationAlreadyInGroup().url
          )

          groupMemberSize(getLastSavedRegistration) mustBe 0
          groupError(getLastSavedRegistration) mustBe Some(
            GroupError(MEMBER_IS_NOMINATED,
                       registration.organisationDetails.incorporationDetails.get.companyName
            )
          )

        }
      }

      "throw exception" when {

        "DB registration update fails " + orgType in {
          authorizedUser()
          mockRegistrationFind(registrationWithSelectedGroupMember(orgType))
          mockGetUkCompanyDetails(incorporationDetails)
          mockRegistrationUpdateFailure()

          intercept[DownstreamServiceError] {
            await(controller.grsCallback("uuid-id")(getRequest()))
          }
        }

        "GRS call fails " + orgType in {
          authorizedUser()
          mockRegistrationFind(registrationWithSelectedGroupMember(orgType))
          mockGetUkCompanyDetailsFailure(new RuntimeException("error"))

          intercept[RuntimeException] {
            await(controller.grsCallback("uuid-id")(getRequest()))
          }
        }

      }

      "show duplicate subscription page" when {
        "a subscription already exists for the group member " + orgType in {
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
          val registration = registrationWithSelectedGroupMember(orgType)
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

    "throw exception" when {
      forAll(unsupportedOrgTypes) { orgType =>
        "invalid organisation type is selected " + orgType in {

          authorizedUser()
          mockRegistrationFind(registrationWithSelectedGroupMember(orgType))

          intercept[IllegalStateException] {
            await(controller.grsCallback("uuid-id")(getRequest()))
          }
        }
      }

      "groupDetail is None" in {
        authorizedUser()
        mockRegistrationFind(aRegistration(withGroupDetail(None)))

        intercept[IllegalStateException] {
          await(controller.grsCallback("uuid-id")(getRequest()))
        }
      }

    }

  }

  private def groupMemberSize(registration: Registration): Int =
    registration.groupDetail.map(_.members.size).getOrElse(0)

  private def groupError(registration: Registration) =
    registration.groupDetail.flatMap(_.groupError)

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
