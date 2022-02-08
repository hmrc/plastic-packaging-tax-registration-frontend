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
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.member_contact_check_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

class ContactDetailsCheckAnswersControllerSpec
    extends ControllerSpec with DefaultAwaitTimeout with PptTestData
    with MockAmendmentJourneyAction {

  private val page = mock[member_contact_check_answers_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsCheckAnswersController(authenticate = mockAuthAllowEnrolmentAction,
                                             amendmentJourneyAction = mockAmendmentJourneyAction,
                                             mcc = mcc,
                                             page = page
    )

  private val populatedRegistration = aRegistration(
    withGroupDetail(groupDetail = Some(groupDetails.copy(members = Seq(groupMember))))
  )

  override protected def beforeEach(): Unit = {
    authorisedUserWithPptSubscription()
    simulateGetSubscriptionSuccess(populatedRegistration)
    when(page.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("Group member contact details check answers")
    )
  }

  override protected def afterEach(): Unit = {
    reset(mockSubscriptionConnector, page)
    super.afterEach()
  }

  "Amend Contact Details Check Answers Controller" should {
    "display group member details" in {
      val resp = controller.displayPage(groupMember.id)(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "Group member contact details check answers"
    }

    "throw exception" when {
      "user not authorised" in {
        unAuthorizedUser()

        intercept[RuntimeException] {
          await(controller.displayPage(groupMember.id)(getRequest()))
        }
      }
      "group member missing from registration" in {
        intercept[IllegalStateException] {
          await(controller.displayPage("123")(getRequest()))
        }
      }
    }

    "redirect to group members list page" when {
      "submitted" in {
        val resp = controller.submit()(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.GroupMembersListController.displayPage().url)
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
