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
import models.registration.Registration
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{verify, when}
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.Html
import views.html.amendment.group.manage_group_members_page

import scala.concurrent.Future

class ManageGroupMembersControllerSpec extends ControllerSpec with AmendmentControllerSpec {

  val registration: Registration = aRegistration()

  val view: manage_group_members_page = mock[manage_group_members_page]

  when(view.apply(any())(any(), any())).thenReturn(Html("view"))

  val sut = new ManageGroupMembersController(spyJourneyAction, Helpers.stubMessagesControllerComponents(), view)

  "displayPage" must {
    "return 200 with view" in {
      spyJourneyAction.setReg(registration)

      val result: Future[Result] = sut.displayPage()(FakeRequest())

      status(result) shouldBe OK
      contentAsString(result) shouldBe "view"
      verify(view).apply(refEq(registration))(any(), any())
      verify(spyJourneyAction).amend
    }
  }
}
