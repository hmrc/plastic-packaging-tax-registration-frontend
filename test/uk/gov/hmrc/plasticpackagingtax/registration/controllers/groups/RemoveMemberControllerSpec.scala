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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.groups

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{await, contentAsString, status}
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.forms.groups.RemoveMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{
  AddressDetails,
  GroupMember
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{GroupDetail, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.groups.remove_group_member_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RemoveMemberControllerSpec extends ControllerSpec {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[remove_group_member_page]

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

    authorizedUser()
    mockRegistrationFind(aRegistration())

    "display page" in {
      val result = removeMemberController.displayPage("123")(getRequest())

      status(result) mustBe OK
      contentAsString(result) mustBe "Remove Member Page"
    }

    "remove identified group member when remove action confirmed" in {
      val groupMember1 = aGroupMember("123")
      val groupMember2 = aGroupMember("456")
      val groupRegistration = aRegistration(
        withGroupDetail(Some(GroupDetail(members = List(groupMember1, groupMember2))))
      )

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
      val groupMember1 = aGroupMember("123")
      val groupMember2 = aGroupMember("456")
      val groupRegistration = aRegistration(
        withGroupDetail(Some(GroupDetail(members = List(groupMember1, groupMember2))))
      )

      mockRegistrationFind(groupRegistration)
      mockRegistrationUpdate()

      await(
        removeMemberController.submit(s"${groupMember1.id}xxx")(
          postJsonRequestEncoded(("value", "yes"))
        )
      )

      verify(mockRegistrationConnector).update(ArgumentMatchers.eq(groupRegistration))(any())
    }

    "display validation error" when {
      "no selection made" in {
        val initialRegistration = aRegistration()
        mockRegistrationFind(initialRegistration)
        mockRegistrationUpdate()

        val emptySelection = Seq()
        val result =
          removeMemberController.submit("123")(postJsonRequestEncoded(emptySelection: _*))

        status(result) mustBe BAD_REQUEST
      }
    }
  }

  private def aGroupMember(id: String) =
    GroupMember(id = id,
                customerIdentification1 = "ABC",
                addressDetails = AddressDetails(addressLine1 = "addressLine1",
                                                addressLine2 = "addressLine2",
                                                countryCode = "UK"
                )
    )

}
