/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.Html
import forms.contact.Address
import forms.group.RemoveMember
import models.registration.group.{GroupMember, OrganisationDetails}
import models.registration.{GroupDetail, Registration}
import org.mockito.Mockito.never
import org.mockito.MockitoSugar.{reset, verify, when}
import play.api.test.FakeRequest
import views.html.group.remove_group_member_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RemoveMemberControllerSpec extends ControllerSpec {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[remove_group_member_page]

  private val groupMember1 = aGroupMember("123")
  private val groupMember2 = aGroupMember("456")

  private val groupRegistration = aRegistration(
    withGroupDetail(Some(GroupDetail(members = List(groupMember1, groupMember2))))
  )

  private val removeMemberController = new RemoveMemberController(journeyAction = spyJourneyAction,
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

      spyJourneyAction.setReg(groupRegistration)

      val result = removeMemberController.displayPage(groupMember1.id)(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) mustBe "Remove Member Page"
    }

    "redirect to group member list when group member not found in registration" in {

      spyJourneyAction.setReg(groupRegistration)

      val result = removeMemberController.displayPage(s"${groupMember1.id}xxx")(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
    }

    "remove identified group member when remove action confirmed" in {

      spyJourneyAction.setReg(groupRegistration)
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

      spyJourneyAction.setReg(groupRegistration)
      mockRegistrationUpdate()

      await(
        removeMemberController.submit(s"${groupMember1.id}xxx")(
          postJsonRequestEncoded(("value", "yes"))
        )
      )

      verify(mockRegistrationConnector, never()).update(any())(any())
    }

    "redirect to group member list when remove confirmation is negative" in {

      spyJourneyAction.setReg(groupRegistration)

      val result =
        removeMemberController.submit(groupMember1.id)(postJsonRequestEncoded(("value", "no")))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
    }

    "redirect to group member list when member removal fails" in {

      spyJourneyAction.setReg(groupRegistration)

      mockRegistrationUpdateFailure()

      val result =
        removeMemberController.submit(groupMember1.id)(postJsonRequestEncoded(("value", "yes")))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.OrganisationListController.displayPage().url)
    }

    "display validation error" when {

      "no selection made" in {

        spyJourneyAction.setReg(groupRegistration)

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
                addressDetails = Address(addressLine1 = "addressLine1",
                                         addressLine2 = Some("addressLine2"),
                                         addressLine3 = None,
                                         townOrCity = "addressLine2",
                                         maybePostcode = Some("postCode"),
                                         countryCode = "UK"
                )
    )

}
