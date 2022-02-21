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

package uk.gov.hmrc.plasticpackagingtax.registration.services

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.UK_COMPANY
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  GroupDetail,
  Registration,
  OrganisationDetails => RegOrgDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionStatus.NOT_SUBSCRIBED

class RegistrationGroupFilterServiceSpec extends PlaySpec with PptTestData with MockitoSugar {

  val sut: RegistrationGroupFilterService = new RegistrationGroupFilterService()

  "removePartialGroupMembers" must {

    "return a registration with only valid group members" in {
      val mockInvalidGroupMember            = createAndSetExpectation(false)
      val mockValidGroupMember: GroupMember = createAndSetExpectation(true)

      val reg = createDefaultOrganisationalDetails(mockValidGroupMember, mockInvalidGroupMember)

      sut.removePartialGroupMembers(reg) mustBe createExpectedResultFrom(reg, mockValidGroupMember)
    }

    "return a registration with no group members" in {
      val mockInvalidGroupMember = createAndSetExpectation(false)

      val reg = createDefaultOrganisationalDetails(mockInvalidGroupMember, mockInvalidGroupMember)

      sut.removePartialGroupMembers(reg) mustBe createExpectedResultFrom(reg)
    }

    "return the current registration if groupDetails does not exist" in {
      val reg = createDefaultOrganisationalDetails().copy(groupDetail = None)

      sut.removePartialGroupMembers(reg) mustBe reg
    }
  }

  private def createAndSetExpectation(boolean: Boolean): GroupMember = {
    val mockValidGroupMember = mock[GroupMember]
    when(mockValidGroupMember.isValid).thenReturn(boolean)
    mockValidGroupMember
  }

  def createDefaultOrganisationalDetails(groupMembers: GroupMember*): Registration =
    aRegistration(
      withOrganisationDetails(
        RegOrgDetails(organisationType = Some(UK_COMPANY),
                      incorporationDetails =
                        Some(
                          IncorporationDetails(companyNumber = "123456",
                                               companyName = "NewPlastics",
                                               ctutr = "1890894",
                                               companyAddress = testCompanyAddress,
                                               registration =
                                                 Some(registrationDetails)
                          )
                        ),
                      subscriptionStatus = Some(NOT_SUBSCRIBED)
        )
      ),
      withGroupDetail(
        Some(GroupDetail(membersUnderGroupControl = Some(true), members = groupMembers))
      )
    )

  def createExpectedResultFrom(reg: Registration, members: GroupMember*): Registration =
    reg.copy(groupDetail =
      Some(GroupDetail(membersUnderGroupControl = Some(true), members = members))
    )

}
