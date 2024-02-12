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

package controllers.amendment.group

import base.unit.{AmendmentControllerSpec, ControllerSpec}
import models.registration.GroupDetail
import models.registration.group.GroupMember
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.amendment.group.confirm_remove_member_page

import java.util.UUID

class ConfirmRemoveMemberControllerSpec
    extends ControllerSpec with AmendmentControllerSpec with TableDrivenPropertyChecks {

  private val mcc = stubMessagesControllerComponents()
  private val page = mock[confirm_remove_member_page]

  private val sessionId = UUID.randomUUID().toString

  private val controller =
    new ConfirmRemoveMemberController(spyJourneyAction, mockAmendRegService, mcc, page)

  private val anotherGroupMember = groupMember.copy(id = "another-group-member-id")
  private val groupMembers: Seq[GroupMember] = Seq(groupMember, anotherGroupMember)

  private val populatedGroupDetails =
    GroupDetail(membersUnderGroupControl = Some(true), members = groupMembers)

  private val populatedRegistrationWithGroupMembers =
    aRegistration().copy(groupDetail = Some(populatedGroupDetails))

  override protected def beforeEach(): Unit = {
    spyJourneyAction.reset()
    reset(spyJourneyAction, mockAmendRegService)
    when(page.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.raw("Confirm remove member"))
  }

  "Confirm Remove Member Controller" when {
    "signed in user has a ppt registration to amend" should {
      "show page" in {
        spyJourneyAction.setReg(populatedRegistrationWithGroupMembers)

        val resp =
          controller.displayPage(groupMember.id)(FakeRequest())

        status(resp) mustBe OK
        verify(spyJourneyAction).amend
      }

      "update registration" when {
        "signed in user when a ppt registration requests removal of an existing group member" in {
          spyJourneyAction.setReg(populatedRegistrationWithGroupMembers)
          simulateUpdateWithRegSubscriptionSuccess()

          // postRequestEncoded will not encode a yes form correctly so we have to manually construct this
          val correctlyEncodedForm = Seq("value" -> "yes")
          val resp = controller.submit(groupMember.id)(
            postRequestTuplesEncoded(correctlyEncodedForm, sessionId)
          )

          status(resp) mustBe SEE_OTHER
          verify(mockAmendRegService).updateSubscriptionWithRegistration(any())(any(), any())
          redirectLocation(resp) mustBe Some(routes.GroupMembersListController.displayPage().url)
          verify(spyJourneyAction).amend
        }
      }
    }
  }

  "return an error" when {

    "user tries to display an non existent group member" in {
      spyJourneyAction.setReg(populatedRegistrationWithGroupMembers)

      intercept[RuntimeException](status(controller.displayPage("not-an-existing-group-member-id")(FakeRequest())))
    }
  }

}
