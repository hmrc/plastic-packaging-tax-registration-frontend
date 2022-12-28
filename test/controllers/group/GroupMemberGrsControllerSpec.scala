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

package controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.dsl.MatcherWords
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{await, redirectLocation, status}
import connectors.DownstreamServiceError
import forms.organisation.OrgType
import forms.organisation.OrgType.{OrgType, PARTNERSHIP}
import models.genericregistration.{CompanyProfile, IncorporationDetails}
import models.registration.group.{GroupMember, OrganisationDetails => GroupOrgDetails}
import models.registration.{GroupDetail, NewRegistrationUpdateService, Registration}
import models.subscriptions.SubscriptionStatus.{NOT_SUBSCRIBED, SUBSCRIBED}
import models.subscriptions.SubscriptionStatusResponse
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class GroupMemberGrsControllerSpec extends ControllerSpec with MatcherWords {

  private val mcc = stubMessagesControllerComponents()

  protected val mockNewRegistrationUpdater = new NewRegistrationUpdateService(mockRegistrationConnector)

  private val controller =
    new GroupMemberGrsController(
      authenticate = mockAuthAction,
      mockJourneyAction,
      mockUkCompanyGrsConnector,
      mockSubscriptionsConnector,
      mockPartnershipGrsConnector,
      mockNewRegistrationUpdater,
      addressConversionUtils,
      mcc
    )(ec)

  private def registrationWithSelectedGroupMember(orgType: OrgType, existingMembers: Seq[GroupMember] = Seq.empty) =
    aRegistration(
      withGroupDetail(
        Some(GroupDetail(membersUnderGroupControl = Some(true), members = existingMembers, currentMemberOrganisationType = Some(orgType)))
      )
    )

  private val supportedOrgTypes =
    Seq(OrgType.UK_COMPANY, OrgType.OVERSEAS_COMPANY_UK_BRANCH, OrgType.PARTNERSHIP)

  private val unsupportedOrgTypes = OrgType.values.filterNot(supportedOrgTypes.contains)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    mockGetSubscriptionStatus(SubscriptionStatusResponse(NOT_SUBSCRIBED, None))
  }

  override protected def afterEach(): Unit = {
    reset(mockUkCompanyGrsConnector, mockRegistrationConnector, mockSubscriptionsConnector, mockPartnershipGrsConnector)
    super.afterEach()
  }

  "group member incorpIdCallback" should {

    forAll(supportedOrgTypes) { orgType =>
      "redirect to the confirm address page" when {

        s"registering a new $orgType member" in {
          val result = orgType match {
            case PARTNERSHIP =>
              simulatePartnershipCallback(registrationWithSelectedGroupMember(orgType))
            case _ =>
              simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(orgType))
          }
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get should include("confirm-address")
        }

        s"registering an existing $orgType member" in {
          val result = orgType match {
            case PARTNERSHIP =>
              simulatePartnershipCallback(registrationWithSelectedGroupMember(orgType, Seq(groupMember)), Some(groupMember.id))
            case _ =>
              simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(orgType, Seq(groupMember)), Some(groupMember.id))
          }
          status(result) mustBe SEE_OTHER
          redirectLocation(result).get should include("confirm-address")
        }
      }

      "store the returned business entity information" when {
        s"registering a $orgType group member" in {
          orgType match {
            case PARTNERSHIP =>
              await(simulatePartnershipCallback(registrationWithSelectedGroupMember(orgType)))
              groupMemberSize(getLastSavedRegistration) mustBe 1

              val memberDetails: GroupMember = getLastSavedRegistration.groupDetail.get.members.last
              val partnershipDetailsWithCompanyProfile =
                partnershipBusinessDetails.copy(companyProfile = Some(companyProfile))
              memberDetails.organisationDetails.get.organisationType mustBe orgType.toString
              memberDetails.organisationDetails.get.organisationName mustBe partnershipDetailsWithCompanyProfile.companyProfile.get.companyName
              memberDetails.addressDetails mustBe addressConversionUtils.toPptAddress(
                partnershipDetailsWithCompanyProfile.companyProfile.get.companyAddress
              )
            case _ =>
              await(simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(orgType)))
              await(simulatePartnershipCallback(registrationWithSelectedGroupMember(orgType)))
              groupMemberSize(getLastSavedRegistration) mustBe 1

              val memberDetails: GroupMember = getLastSavedRegistration.groupDetail.get.members.last

              memberDetails.organisationDetails.get.organisationType mustBe orgType.toString
              memberDetails.organisationDetails.get.organisationName mustBe incorporationDetails.companyName
              memberDetails.addressDetails mustBe addressConversionUtils.toPptAddress(incorporationDetails.companyAddress)
          }

        }

      }

      "change an add member to a change member journey" when {
        s"registering a $orgType group member with same company number as another group member" in {
          authorizedUser()
          val existingMember = orgType match {
            case PARTNERSHIP =>
              val detailsFromGrs =
                partnershipBusinessDetails.copy(companyProfile =
                  Some(CompanyProfile(companyNumber = "22222", companyName = "Existing Company", companyAddress = testCompanyAddress))
                )
              mockGetPartnershipBusinessDetails(detailsFromGrs)
              groupMember.copy(
                customerIdentification1 = "22222",
                organisationDetails =
                  Some(GroupOrgDetails("UkCompany", "Existing Company", Some(safeNumber)))
              )
            case _ =>
              val detailsFromGrs =
                incorporationDetails.copy(companyNumber = "22222", companyName = "Existing Company")
              mockGetUkCompanyDetails(detailsFromGrs)
              groupMember.copy(
                customerIdentification1 = "22222",
                organisationDetails =
                  Some(GroupOrgDetails("UkCompany", "Existing Company", Some(safeNumber)))
              )

          }

          val registration = registrationWithSelectedGroupMember(orgType, Seq(existingMember))
          mockRegistrationFind(registration)
          mockRegistrationUpdate()

          groupMemberSize(registration) mustBe 1

          val result = controller.grsCallbackNewMember(registration.incorpJourneyId.get)(getRequest())

          status(result) mustBe SEE_OTHER

          if(orgType.equals(OrgType.PARTNERSHIP)){
            redirectLocation(result).get should include("/register-for-plastic-packaging-tax/confirm-address")
          } else {
            redirectLocation(result) mustBe Some(routes.ConfirmBusinessAddressController.displayPage(
              "123456ABC", "/register-for-plastic-packaging-tax/group-member-contact-name/123456ABC").url
            )
          }

          groupMemberSize(getLastSavedRegistration) mustBe 1

        }
      }

      "throw exception" when {

        "DB registration update fails " + orgType in {
          authorizedUser()
          orgType match {
            case PARTNERSHIP =>
              mockGetPartnershipBusinessDetails(partnershipBusinessDetails.copy(companyProfile = Some(companyProfile)))
            case _ =>
              mockGetUkCompanyDetails(incorporationDetails)
          }
          mockRegistrationFind(registrationWithSelectedGroupMember(orgType))
          mockRegistrationUpdateFailure()

          intercept[DownstreamServiceError] {
            await(controller.grsCallbackNewMember("uuid-id")(getRequest()))
          }

        }

        "GRS call fails " + orgType in {
          authorizedUser()
          mockRegistrationFind(registrationWithSelectedGroupMember(orgType))
          mockGetUkCompanyDetailsFailure(new RuntimeException("error"))

          intercept[RuntimeException] {
            await(controller.grsCallbackNewMember("uuid-id")(getRequest()))
          }
        }

      }

      "show duplicate subscription page" when {
        "a subscription already exists for the group member " + orgType in {
          authorizedUser()
          orgType match {
            case PARTNERSHIP =>
              mockGetPartnershipBusinessDetails(partnershipBusinessDetails.copy(companyProfile = Some(companyProfile)))
            case _ =>
              mockGetUkCompanyDetails(IncorporationDetails("123467890", testCompanyName, Some(testUtr), testCompanyAddress, Some(registrationDetails)))
          }
          val registration = registrationWithSelectedGroupMember(orgType)
          mockRegistrationFind(registration)
          mockRegistrationUpdate()
          mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))

          val result =
            controller.grsCallbackNewMember(registration.incorpJourneyId.get)(getRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.NotableErrorController.groupMemberAlreadyRegistered().url)

        }
      }
    }

    "throw exception" when {
      forAll(unsupportedOrgTypes) { orgType =>
        "invalid organisation type is selected " + orgType in {

          authorizedUser()
          mockRegistrationFind(registrationWithSelectedGroupMember(orgType))

          intercept[IllegalStateException] {
            await(controller.grsCallbackNewMember("uuid-id")(getRequest()))
          }
        }
      }

      "groupDetail is None" in {
        authorizedUser()
        mockRegistrationFind(aRegistration(withGroupDetail(None)))

        intercept[IllegalStateException] {
          await(controller.grsCallbackNewMember("uuid-id")(getRequest()))
        }
      }

    }

  }

  private def groupMemberSize(registration: Registration): Int =
    registration.groupDetail.map(_.members.size).getOrElse(0)

  private def simulateLimitedCompanyCallback(registration: Registration, memberId: Option[String] = None) = {
    authorizedUser()
    mockGetUkCompanyDetails(IncorporationDetails("123467890", testCompanyName, Some(testUtr), testCompanyAddress, Some(registrationDetails)))
    mockRegistrationFind(registration)
    mockRegistrationUpdate()

    memberId match {
      case Some(memberId) =>
        controller.grsCallbackAmendMember(registration.incorpJourneyId.get, memberId)(getRequest())
      case None => controller.grsCallbackNewMember(registration.incorpJourneyId.get)(getRequest())
    }
  }

  private def simulatePartnershipCallback(registration: Registration, memberId: Option[String] = None) = {
    authorizedUser()
    mockGetPartnershipBusinessDetails(
      partnershipBusinessDetails.copy(companyProfile =
        Some(companyProfile.copy(companyNumber = "234234234"))
      )
    )
    mockRegistrationFind(registration)
    mockRegistrationUpdate()

    memberId match {
      case Some(memberId) =>
        controller.grsCallbackAmendMember(registration.incorpJourneyId.get, memberId)(getRequest())
      case None => controller.grsCallbackNewMember(registration.incorpJourneyId.get)(getRequest())
    }
  }

  private def getLastSavedRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector, Mockito.atLeastOnce()).update(captor.capture())(any())
    captor.getValue
  }

}
