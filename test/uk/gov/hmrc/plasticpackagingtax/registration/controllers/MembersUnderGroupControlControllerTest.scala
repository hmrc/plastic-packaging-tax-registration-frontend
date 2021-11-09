/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.MembersUnderGroupControl
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.members_under_group_control_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class MembersUnderGroupControlControllerTest extends ControllerSpec {

  private val page = mock[members_under_group_control_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new MembersUnderGroupControlController(authenticate = mockAuthAction,
                                           mcc = mcc,
                                           mockJourneyAction,
                                           page = page,
                                           mockRegistrationConnector
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[MembersUnderGroupControl]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "MembersUnderGroupControl Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user has previously selected an answer and display page method is invoked" in {
        authorizedUser()
        val registration =
          aRegistration(withGroupDetail(Some(GroupDetail(membersUnderGroupControl = Some(true)))))
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "redirect to check your answers page" when {
      "user selects 'yes'" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val correctForm = Seq("value" -> "yes", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(
          routes.CheckLiabilityDetailsAnswersController.displayPage().url
        )
      }
    }

    "redirect to not not able to apply as a group page " when {
      "user selects 'no'" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val correctForm = Seq("value" -> "no", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(
          routes.NotMembersUnderGroupControlController.displayPage().url
        )
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "display a BAD REQUEST status" when {
        "no selection is made" in {
          authorizedUser()
          val result = controller.submit()(postRequestEncoded(MembersUnderGroupControl(None)))

          status(result) mustBe BAD_REQUEST
        }
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationUpdateFailure()
        val correctForm = Seq("value" -> "no", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        intercept[DownstreamServiceError](status(result))
      }
    }
  }
}
