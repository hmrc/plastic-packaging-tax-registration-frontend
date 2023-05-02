/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.amendment.group
import models.registration.Registration
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import views.html.amendment.group.list_group_members_page
import views.viewmodels.ListGroupMembersViewModel

import scala.concurrent.Future

class GroupMembersListControllerSpec
    extends ControllerSpec with AmendmentControllerSpec with BeforeAndAfterEach {

  val registration: Registration = aRegistration()

  val view: list_group_members_page = mock[list_group_members_page]

  when(view.apply(any(), any())(any(), any())).thenReturn(Html("view"))

  val sut = new GroupMembersListController(journeyAction = spyJourneyAction,
                                           stubMessagesControllerComponents(),
                                           view
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    spyJourneyAction.setReg(registration)
  }

  val viewModel = new ListGroupMembersViewModel(registration)

  "displayPage" must {
    "return 200 with view" in {
      val result: Future[Result] =
        sut.displayPage()(FakeRequest())

      status(result) shouldBe OK
      contentAsString(result) shouldBe "view"
      verify(view).apply(any(), refEq(viewModel))(any(), any())
    }
  }

  "onSubmit" must {
    "bind the form and redirect" when {
      "submitted yes" in {
        val request: Request[AnyContent] =
          postRequest.withFormUrlEncodedBody("addOrganisation" -> "yes").withCSRFToken

        val result: Future[Result] = sut.onSubmit()(request)

        redirectLocation(result) shouldBe Some(
          group.routes.AddGroupMemberOrganisationDetailsTypeController.displayPage().url
        )
      }

      "submitted no" in {
        val myRequest =
          postRequest.withFormUrlEncodedBody("addOrganisation" -> "no").withCSRFToken

        val result: Future[Result] = sut.onSubmit()(myRequest)

        redirectLocation(result) shouldBe Some(
          routes.ManageGroupMembersController.displayPage().url
        )
      }
    }

    "fail binding and return errored form" when {
      "no form submission" in {
        val request =
          FakeRequest().withCSRFToken

        val result: Future[Result] = sut.onSubmit()(request)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe "view"
      }
      "bad form submission" in {
        val request = FakeRequest().withFormUrlEncodedBody("addOrganisation" -> "")

        val result: Future[Result] = sut.onSubmit()(request)

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldBe "view"
      }
    }
  }
}
