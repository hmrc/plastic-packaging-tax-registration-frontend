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
import play.api.http.Status.OK
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupMember
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.confirm_remove_member_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

class ConfirmRemoveMemberControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction with TableDrivenPropertyChecks {

  private val mcc  = stubMessagesControllerComponents()
  private val page = mock[confirm_remove_member_page]

  private val controller =
    new ConfirmRemoveMemberController(mockAuthAllowEnrolmentAction,
                                      mockAmendmentJourneyAction,
                                      mockRegistrationConnector,
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

  "Confirm Remove Member Controller" should {
    "show page" when {
      "signed in user has a ppt registration to amend" in { // TODO what if it isn't a group registration?
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionSuccess(populatedRegistrationWithGroupMembers)

        val resp =
          controller.displayPage(groupMember.id)(getRequest())

        status(resp) mustBe OK
      }
    }
  }

  "return an error" when {

    "user is not authorised" in {
      unAuthorizedUser()

      val result = controller.displayPage(groupMember.id)(getRequest())

      intercept[RuntimeException](status(result))
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
