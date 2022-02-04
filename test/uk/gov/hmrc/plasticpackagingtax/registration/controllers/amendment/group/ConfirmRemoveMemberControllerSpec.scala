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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group

import base.PptTestData.newUser
import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.RemoveMember
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.RemoveMember.fromForm
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.confirm_remove_member_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import java.util.UUID

class ConfirmRemoveMemberControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction with TableDrivenPropertyChecks {

  private val mcc  = stubMessagesControllerComponents()
  private val page = mock[confirm_remove_member_page]

  private val sessionId = UUID.randomUUID().toString

  private val controller =
    new ConfirmRemoveMemberController(mockAuthAllowEnrolmentAction,
                                      mockAmendmentJourneyAction,
                                      mcc,
                                      page
    )

  private def authorisedUserWithPptSubscription() =
    authorizedUser(user =
      newUser().copy(enrolments =
        Enrolments(
          Set(
            new Enrolment(PptEnrolment.Identifier,
                          Seq(EnrolmentIdentifier(PptEnrolment.Key, "XMPPT0000000123")),
                          "activated"
            )
          )
        )
      )
    )

  val anotherGroupMember                     = groupMember.copy(id = "another-group-member-id")
  private val groupMembers: Seq[GroupMember] = Seq(groupMember, anotherGroupMember)

  val populatedGroupDetails =
    GroupDetail(membersUnderGroupControl = Some(true), members = groupMembers)

  private val populatedRegistrationWithGroupMembers =
    aRegistration().copy(groupDetail = Some(populatedGroupDetails))

  override protected def beforeEach(): Unit = {
    inMemoryRegistrationAmendmentRepository.reset()
    reset(mockSubscriptionConnector)
    simulateGetSubscriptionSuccess(populatedRegistrationWithGroupMembers)
    when(page.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.raw("Confirm remove member"))
  }

  "Confirm Remove Member Controller" when {
    "signed in user has a ppt registration to amend" should {
      "show page" in {
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionSuccess(populatedRegistrationWithGroupMembers)

        val resp =
          controller.displayPage(groupMember.id)(getRequest())

        status(resp) mustBe OK
      }

      "update registration" when {
        "signed in user when a ppt registration requests removal of an existing group member" in {
          authorisedUserWithPptSubscription()
          simulateGetSubscriptionSuccess(populatedRegistrationWithGroupMembers)

          // postRequestEncoded will not encode a yes form correctly
          val correctlyEncodedForm = Seq("value" -> "yes")
          val resp = controller.submit(groupMember.id)(
            postRequestTuplesEncoded(correctlyEncodedForm, continueFormAction, sessionId)
          )

          status(resp) mustBe SEE_OTHER
          val updatedReg = await(inMemoryRegistrationAmendmentRepository.get(sessionId)).get
          updatedReg.groupDetail.get.members.size mustBe 1
          updatedReg.groupDetail.get.members.head mustBe groupMember
        }
      }
    }
  }

  "return an error" when {

    "user is not authorised" when {
      "tries to display page" in {
        unAuthorizedUser()

        val result = controller.displayPage(groupMember.id)(getRequest())

        intercept[RuntimeException](status(result))
      }

      "tries to submit" in {
        unAuthorizedUser()

        val result = controller.submit(groupMember.id)(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    "user tries to display an non existent group member" in {
      authorisedUserWithPptSubscription()

      val result = controller.displayPage("not-an-existing-group-member-id")(getRequest())

      intercept[RuntimeException](status(result))
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
