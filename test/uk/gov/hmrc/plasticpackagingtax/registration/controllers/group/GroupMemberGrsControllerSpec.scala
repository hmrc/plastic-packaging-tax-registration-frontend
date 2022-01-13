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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  OrgType,
  PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  CompanyProfile,
  IncorporationDetails
}
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
                                 mockPartnershipGrsConnector,
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

  private val supportedOrgTypes =
    Seq(OrgType.UK_COMPANY, OrgType.OVERSEAS_COMPANY_UK_BRANCH, OrgType.PARTNERSHIP)

  private val unsupportedOrgTypes = OrgType.values.filterNot(supportedOrgTypes.contains)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    mockGetSubscriptionStatus(SubscriptionStatusResponse(NOT_SUBSCRIBED, None))
  }

  override protected def afterEach(): Unit = {
    reset(mockUkCompanyGrsConnector,
          mockRegistrationConnector,
          mockSubscriptionsConnector,
          mockPartnershipGrsConnector
    )
    super.afterEach()
  }

  private def stripMemberId(url: String) = url.substring(0, url.lastIndexOf("/"))

  "group member incorpIdCallback" should {

    forAll(supportedOrgTypes) { orgType =>
      "redirect to the group member name details page" when {

        s"registering a new $orgType member" in {
          val result = orgType match {
            case PARTNERSHIP =>
              simulatePartnershipCallback(registrationWithSelectedGroupMember(orgType))
            case _ =>
              simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(orgType))
          }
          status(result) mustBe SEE_OTHER
          stripMemberId(redirectLocation(result).get) mustBe stripMemberId(
            routes.ContactDetailsNameController.displayPage(groupMember.id).url
          )
        }

        s"registering an existing $orgType member" in {
          val result = orgType match {
            case PARTNERSHIP =>
              simulatePartnershipCallback(registrationWithSelectedGroupMember(orgType,
                                                                              Seq(groupMember)
                                          ),
                                          Some(groupMember.id)
              )
            case _ =>
              simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(orgType,
                                                                                 Seq(groupMember)
                                             ),
                                             Some(groupMember.id)
              )
          }
          status(result) mustBe SEE_OTHER
          stripMemberId(redirectLocation(result).get) mustBe stripMemberId(
            routes.ContactDetailsNameController.displayPage(groupMember.id).url
          )
        }
      }

      "store the returned business entity information" when {
        "registering group member " + orgType in {
          orgType match {
            case PARTNERSHIP =>
              await(simulatePartnershipCallback(registrationWithSelectedGroupMember(orgType)))
              groupMemberSize(getLastSavedRegistration) mustBe 1

              val memberDetails: GroupMember = getLastSavedRegistration.groupDetail.get.members.last
              val partnershipDetailsWithCompanyProfile =
                partnershipBusinessDetails.copy(companyProfile = Some(companyProfile))
              memberDetails.organisationDetails.get.organisationType mustBe orgType.toString
              memberDetails.organisationDetails.get.organisationName mustBe partnershipDetailsWithCompanyProfile.companyProfile.get.companyName
              memberDetails.addressDetails mustBe partnershipDetailsWithCompanyProfile.companyProfile.get.companyAddress.toPptAddress
            case _ =>
              await(simulateLimitedCompanyCallback(registrationWithSelectedGroupMember(orgType)))
              await(simulatePartnershipCallback(registrationWithSelectedGroupMember(orgType)))
              groupMemberSize(getLastSavedRegistration) mustBe 1

              val memberDetails: GroupMember = getLastSavedRegistration.groupDetail.get.members.last

              memberDetails.organisationDetails.get.organisationType mustBe orgType.toString
              memberDetails.organisationDetails.get.organisationName mustBe incorporationDetails.companyName
              memberDetails.addressDetails mustBe incorporationDetails.companyAddress.toPptAddress
          }

        }

      }

      "show error page and not store business entity information " when {
        "registering with same company number " + orgType in {
          authorizedUser()
          val existingMember = orgType match {
            case PARTNERSHIP =>
              val detailsFromGrs =
                partnershipBusinessDetails.copy(companyProfile =
                  Some(
                    CompanyProfile(companyNumber = "22222",
                                   companyName = "Existing Company",
                                   companyAddress = testCompanyAddress
                    )
                  )
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
          val result =
            controller.grsCallbackNewMember(registration.incorpJourneyId.get)(getRequest())

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
          var registration: Registration = registrationWithSelectedGroupMember(orgType)

          orgType match {
            case PARTNERSHIP =>
              registration = registration.copy(organisationDetails =
                registeredLimitedLiabilityhPartnershipOrgDetails()
              )
              mockRegistrationFind(registration)
              mockRegistrationUpdate()
              groupMemberSize(registration) mustBe 0
              val updatedPartnershipBusinessDetails =
                partnershipBusinessDetails.copy(companyProfile =
                  Some(
                    CompanyProfile(
                      companyNumber =
                        registration.organisationDetails.partnershipDetails.get.partnershipBusinessDetails.get.companyProfile.get.companyNumber,
                      companyName =
                        registration.organisationDetails.partnershipDetails.get.partnershipBusinessDetails.get.companyProfile.get.companyName,
                      companyAddress =
                        registration.organisationDetails.partnershipDetails.get.partnershipBusinessDetails.get.companyProfile.get.companyAddress
                    )
                  )
                )
              mockGetPartnershipBusinessDetails(updatedPartnershipBusinessDetails)
              val result =
                controller.grsCallbackNewMember(registration.incorpJourneyId.get)(getRequest())

              status(result) mustBe SEE_OTHER
              redirectLocation(result) mustBe Some(
                routes.NotableErrorController.organisationAlreadyInGroup().url
              )

              groupMemberSize(getLastSavedRegistration) mustBe 0
              groupError(getLastSavedRegistration) mustBe Some(
                GroupError(
                  MEMBER_IS_NOMINATED,
                  registration.organisationDetails.partnershipDetails.get.partnershipBusinessDetails.get.companyProfile.get.companyName
                )
              )
            case _ =>
              registration = registrationWithSelectedGroupMember(orgType)
              mockRegistrationFind(registration)
              mockRegistrationUpdate()
              groupMemberSize(registration) mustBe 0
              val detailsFromGrs = incorporationDetails.copy(
                companyNumber =
                  registration.organisationDetails.incorporationDetails.get.companyNumber,
                companyName = registration.organisationDetails.incorporationDetails.get.companyName
              )
              mockGetUkCompanyDetails(detailsFromGrs)
              val result =
                controller.grsCallbackNewMember(registration.incorpJourneyId.get)(getRequest())

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
      }

      "throw exception" when {

        "DB registration update fails " + orgType in {
          authorizedUser()
          orgType match {
            case PARTNERSHIP =>
              mockGetPartnershipBusinessDetails(
                partnershipBusinessDetails.copy(companyProfile = Some(companyProfile))
              )
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
              mockGetPartnershipBusinessDetails(
                partnershipBusinessDetails.copy(companyProfile = Some(companyProfile))
              )
            case _ =>
              mockGetUkCompanyDetails(
                IncorporationDetails("123467890",
                                     testCompanyName,
                                     testUtr,
                                     testCompanyAddress,
                                     Some(registrationDetails)
                )
              )
          }
          val registration = registrationWithSelectedGroupMember(orgType)
          mockRegistrationFind(registration)
          mockRegistrationUpdate()
          mockGetSubscriptionStatus(SubscriptionStatusResponse(SUBSCRIBED, Some("XDPPT1234567890")))

          val result =
            controller.grsCallbackNewMember(registration.incorpJourneyId.get)(getRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            routes.NotableErrorController.groupMemberAlreadyRegistered().url
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

  private def groupError(registration: Registration) =
    registration.groupDetail.flatMap(_.groupError)

  private def simulateLimitedCompanyCallback(
    registration: Registration,
    memberId: Option[String] = None
  ) = {
    authorizedUser()
    mockGetUkCompanyDetails(
      IncorporationDetails("123467890",
                           testCompanyName,
                           testUtr,
                           testCompanyAddress,
                           Some(registrationDetails)
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

  private def simulatePartnershipCallback(
    registration: Registration,
    memberId: Option[String] = None
  ) = {
    authorizedUser()
    mockGetPartnershipBusinessDetails(
      partnershipBusinessDetails.copy(companyProfile = Some(companyProfile))
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
