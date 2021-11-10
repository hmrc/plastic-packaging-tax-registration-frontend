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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.groups.OrganisationListController
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.views.groups.html.organisation_list
import uk.gov.hmrc.plasticpackagingtax.registration.{controllers => pptControllers}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class OrganisationListControllerSpec extends ControllerSpec {
  private val page = mock[organisation_list]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new OrganisationListController(authenticate = mockAuthAction,
                                   mockJourneyAction,
                                   mockRegistrationConnector,
                                   mcc = mcc,
                                   page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)

    val registration = aRegistration(
      withGroupDetail(
        Some(GroupDetail(membersUnderGroupControl = Some(true), members = Seq(groupMember)))
      )
    )
    mockRegistrationFind(registration)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Organisation list controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "return 303" when {

      "no group members exist" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        val result = controller.displayPage()(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          pptControllers.routes.RegistrationController.displayPage().url
        )
      }

    }

    "return 400" when {

      "user does not make a selection" in {
        authorizedUser()

        val correctForm = Seq("addOrganisation" -> "", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe BAD_REQUEST

      }

    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    "redirects to registration page" when {
      "user does not want to add another" in {
        authorizedUser()

        val correctForm = Seq("addOrganisation" -> "no", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          pptControllers.routes.RegistrationController.displayPage().url
        )
      }
      "user selects save and come back later" in {
        authorizedUser()

        val correctForm = Seq("addOrganisation" -> "yes", saveAndComeBackLaterFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          pptControllers.routes.RegistrationController.displayPage().url
        )
      }
    }

    "redirects to add group organisation page" when {
      "user does want to add another" in {
        authorizedUser()

        val correctForm = Seq("addOrganisation" -> "yes", saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          pptControllers.groups.routes.OrganisationListController.displayPage().url // TODO fix redirect location
        )
      }
    }
  }

}
