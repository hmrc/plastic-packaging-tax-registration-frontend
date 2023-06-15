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

package controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import models.registration.GroupDetail
import views.html.group.member_contact_check_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsCheckAnswersControllerSpec
    extends ControllerSpec with DefaultAwaitTimeout with PptTestData {

  private val page = mock[member_contact_check_answers_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsCheckAnswersController(journeyAction = spyJourneyAction,
                                             registrationConnector = mockRegistrationConnector,
                                             mcc = mcc,
                                             page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    spyJourneyAction.setReg(
      aRegistration(
        withGroupDetail(
          Some(GroupDetail(membersUnderGroupControl = Some(true), members = Seq(groupMember)))
        )
      )
    )
    when(page.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("Group member contact details check answers")
    )
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Contact Details Check Answers Controller" should {
    "display captured group member details" in {
      val resp = controller.displayPage(groupMember.id)(FakeRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "Group member contact details check answers"
    }

    "throw exception" when {
      "group member missing from registration" in {
        spyJourneyAction.setReg(aRegistration(withGroupDetail(Some(groupDetails))))

        intercept[IllegalStateException] {
          await(controller.displayPage(groupMember.id)(FakeRequest()))
        }
      }
    }

    "redirect to group members list page" when {
      "submitted" in {
        val resp = controller.submit()(FakeRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.OrganisationListController.displayPage().url)
      }
    }
  }
}
