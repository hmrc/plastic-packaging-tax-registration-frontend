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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.RemoveMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  AddressDetails,
  GroupMember,
  OrganisationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{GroupDetail, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.remove_group_member_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RemoveMemberControllerSpec extends ControllerSpec {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[remove_group_member_page]

  private val groupMember1 = aGroupMember("123")
  private val groupMember2 = aGroupMember("456")

  private val groupRegistration = aRegistration(
    withGroupDetail(Some(GroupDetail(members = List(groupMember1, groupMember2))))
  )

  private val removeMemberController = new RemoveMemberController(mockAuthAction,
                                                                  mockJourneyAction,
                                                                  mockRegistrationConnector,
                                                                  mcc,
                                                                  mockPage
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(mockPage.apply(any[Form[RemoveMember]], any(), any())(any(), any())).thenReturn(
      Html("Remove Member Page")
    )
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(mockRegistrationConnector, mockPage)
  }

  "Remove Member Controller" should {

    "display page when group member found in registration" in {
      authorizedUser()
      mockRegistrationFind(groupRegistration)

      val result = removeMemberController.displayPage(groupMember1.id)(getRequest())

      status(result) mustBe OK
      contentAsString(result) mustBe "Remove Member Page"
    }

    "redirect to group member list when group member not found in registration" in {
      authorizedUser()
      mockRegistrationFind(groupRegistration)

      val result = removeMemberController.displayPage(s"${groupMember1.id}xxx")(getRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
    }

    "remove identified group member when remove action confirmed" in {
      authorizedUser()
      mockRegistrationFind(groupRegistration)
      mockRegistrationUpdate()

      await(
        removeMemberController.submit(groupMember1.id)(postJsonRequestEncoded(("value", "yes")))
      )

      val registrationCaptor: ArgumentCaptor[Registration] =
        ArgumentCaptor.forClass(classOf[Registration])
      verify(mockRegistrationConnector).update(registrationCaptor.capture())(any())

      registrationCaptor.getValue.groupDetail.get.members.size mustBe 1
      registrationCaptor.getValue.groupDetail.get.members.head mustBe groupMember2
    }

    "leave registration unchanged when unknown group member id specified" in {
      authorizedUser()
      mockRegistrationFind(groupRegistration)
      mockRegistrationUpdate()

      await(
        removeMemberController.submit(s"${groupMember1.id}xxx")(
          postJsonRequestEncoded(("value", "yes"))
        )
      )

      verify(mockRegistrationConnector, never()).update(any())(any())
    }

    "redirect to group member list when remove confirmation is negative" in {
      authorizedUser()
      mockRegistrationFind(groupRegistration)

      val result =
        removeMemberController.submit(groupMember1.id)(postJsonRequestEncoded(("value", "no")))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
    }

    "redirect to group member list when member removal fails" in {
      authorizedUser()
      mockRegistrationFind(groupRegistration)

      mockRegistrationUpdateFailure()

      val result =
        removeMemberController.submit(groupMember1.id)(postJsonRequestEncoded(("value", "yes")))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
    }

    "display validation error" when {

      "no selection made" in {
        authorizedUser()
        mockRegistrationFind(groupRegistration)

        val emptySelection = Seq()
        val result =
          removeMemberController.submit(groupMember1.id)(postJsonRequestEncoded(emptySelection: _*))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  private def aGroupMember(id: String) =
    GroupMember(id = id,
                customerIdentification1 = "ABC",
                organisationDetails =
                  Some(OrganisationDetails("Limited Company", s"Company $id", Some("12313123"))),
                addressDetails = AddressDetails(addressLine1 = "addressLine1",
                                                addressLine2 = "addressLine2",
                                                countryCode = "UK"
                )
    )

}
