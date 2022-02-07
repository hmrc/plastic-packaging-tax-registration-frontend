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

import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.amendment.group.ListGroupMembersViewModel
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.list_group_members_page

import scala.concurrent.Future

class GroupMembersListControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction with BeforeAndAfterEach {

  val registration: Registration = aRegistration()

  val view: list_group_members_page = mock[list_group_members_page]

  when(view.apply(any(), any())(any(), any())).thenReturn(Html("view"))

  val sut = new GroupMembersListController(mockAuthAllowEnrolmentAction,
                                           mockAmendmentJourneyAction,
                                           stubMessagesControllerComponents(),
                                           view
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    authorisedUserWithPptSubscription()
    simulateGetSubscriptionSuccess(registration)
  }

  val viewModel = new ListGroupMembersViewModel(registration)

  "displayPage" must {
    "return 200 with view" in {
      val result: Future[Result] =
        sut.displayPage()(FakeRequest().withSession(AmendmentJourneyAction.SessionId -> "123"))

      status(result) shouldBe OK
      contentAsString(result) shouldBe "view"
      verify(view).apply(any(), refEq(viewModel))(any(), any())
    }
  }

  "onSubmit" must {
    "bind the form and redirect" when {
      "submitted yes" in {
        val request: Request[AnyContent] =
          FakeRequest().withFormUrlEncodedBody("addOrganisation" -> "yes").withSession(
            AmendmentJourneyAction.SessionId -> "123"
          ).withCSRFToken

        val result: Future[Result] = sut.onSubmit()(request)

        redirectLocation(result) shouldBe Some(
          group.routes.AddGroupMemberOrganisationDetailsTypeController.displayPage().url
        ) //todo update this route, when merged with other ticket.
      }

      "submitted no" in {
        val myRequest =
          FakeRequest().withFormUrlEncodedBody("addOrganisation" -> "no").withSession(
            AmendmentJourneyAction.SessionId -> "123"
          ).withCSRFToken

        val result: Future[Result] = sut.onSubmit()(myRequest)

        redirectLocation(result) shouldBe Some(
          routes.ManageGroupMembersController.displayPage().url
        )
      }
    }

    "fail binding and return errored form" when {
      "no form submission" in {
        val request =
          FakeRequest().withSession(AmendmentJourneyAction.SessionId -> "123").withCSRFToken

        val result: Future[Result] = sut.onSubmit()(request)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe "view"
      }
      "bad form submission" in {
        val request = FakeRequest().withSession(
          AmendmentJourneyAction.SessionId -> "123"
        ).withFormUrlEncodedBody("addOrganisation" -> "")

        val result: Future[Result] = sut.onSubmit()(request)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe "view"
      }
    }
  }
}
