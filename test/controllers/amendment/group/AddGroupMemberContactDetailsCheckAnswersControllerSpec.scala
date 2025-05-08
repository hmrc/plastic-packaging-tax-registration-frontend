/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.amendment.{routes => amendRoutes}
import models.registration.Registration
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.amendment.group.amend_member_contact_check_answers_page

class AddGroupMemberContactDetailsCheckAnswersControllerSpec extends ControllerSpec with AmendmentControllerSpec {

  private val mcc     = stubMessagesControllerComponents()
  private val cyaPage = mock[amend_member_contact_check_answers_page]

  when(cyaPage.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.raw("Amend Reg - New Group Member CYA"))

  private val controller =
    new AddGroupMemberContactDetailsCheckAnswersController(
      journeyAction = spyJourneyAction,
      mcc = mcc,
      page = cyaPage,
      amendRegistrationService = mockAmendRegService
    )

  private val groupRegistrationInAmendment: Registration = aRegistration(withGroupDetail(Some(groupDetailsWithMembers)))

  override protected def beforeEach(): Unit = {
    reset(spyJourneyAction, mockAmendRegService)
    spyJourneyAction.reset()
    simulateUpdateWithRegSubscriptionSuccess()
  }

  "AddGroupMemberContactDetailsCheckAnswersController" when {

    "displaying page" should {
      "display successfully" when {
        "group member present" in {
          spyJourneyAction.setReg(groupRegistrationInAmendment)
          val mostRecentGroupMember = groupRegistrationInAmendment.groupDetail.get.members.last

          val resp = controller.displayPage(mostRecentGroupMember.id)(FakeRequest())

          status(resp) mustBe OK
          contentAsString(resp) mustBe "Amend Reg - New Group Member CYA"
          verify(spyJourneyAction).amend
        }
      }
      "throw IllegalStateException" when {
        "group member is absent" in {
          spyJourneyAction.setReg(groupRegistrationInAmendment)

          intercept[IllegalStateException] {
            await(controller.displayPage("XXX")(FakeRequest()))
          }
        }
      }
    }

    "details confirmed" should {
      "update subscription at the ETMP back end" in {
        spyJourneyAction.setReg(groupRegistrationInAmendment)

        await(controller.submit()(FakeRequest()))

        verify(mockAmendRegService).updateSubscriptionWithRegistration(any())(any(), any())
        verify(spyJourneyAction).amend
      }

      "redirect to the manage group page when update successful" in {
        spyJourneyAction.setReg(groupRegistrationInAmendment)

        val resp = controller.submit()(FakeRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.ManageGroupMembersController.displayPage().toString)
        verify(spyJourneyAction).amend
      }
      "redirect to the post reg amend error page" when {
        "update fails due to exception being thrown" in {
          spyJourneyAction.setReg(groupRegistrationInAmendment)
          simulateUpdateWithRegSubscriptionFailure(new RuntimeException("BANG!"))

          val resp = controller.submit()(FakeRequest())

          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some(
            amendRoutes.AmendRegistrationController.registrationUpdateFailed().toString
          )
          verify(spyJourneyAction).amend
        }
        "update fails due to error returned from ETMP" in {
          spyJourneyAction.setReg(groupRegistrationInAmendment)
          simulateUpdateSubscriptionWithRegFailureReturnedError()

          val resp = controller.submit()(FakeRequest())

          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some(
            amendRoutes.AmendRegistrationController.registrationUpdateFailed().toString
          )
          verify(spyJourneyAction).amend
        }
      }
    }
  }

}
