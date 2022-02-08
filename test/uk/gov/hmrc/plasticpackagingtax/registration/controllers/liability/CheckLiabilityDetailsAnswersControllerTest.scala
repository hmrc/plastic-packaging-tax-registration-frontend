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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability

import base.unit.ControllerSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.libs.json.JsObject
import play.api.mvc.Call
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{
  StartRegistrationController,
  routes => pptRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.check_liability_details_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class CheckLiabilityDetailsAnswersControllerTest extends ControllerSpec {
  private val page                            = mock[check_liability_details_answers_page]
  private val mcc                             = stubMessagesControllerComponents()
  private val mockStartRegistrationController = mock[StartRegistrationController]

  private val startLiabilityLink = Call("GET", "/start-liability")

  private val controller =
    new CheckLiabilityDetailsAnswersController(authenticate = mockAuthAction,
                                               mockJourneyAction,
                                               mcc = mcc,
                                               startRegistrationController =
                                                 mockStartRegistrationController,
                                               page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    val registration = aRegistration(withRegistrationType(Some(RegType.SINGLE_ENTITY)))
    mockRegistrationFind(registration)
    given(page.apply(refEq(registration), any(), any())(any(), any())).willReturn(HtmlFormat.empty)
    when(mockStartRegistrationController.startLink(any())).thenReturn(startLiabilityLink)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Check liability details answers Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" when {
        "and 'isPreLaunch' is true" in {
          when(config.isDefaultFeatureFlagEnabled(refEq(Features.isPreLaunch))).thenReturn(true)
          authorizedUser()
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
        }

        "and 'isPreLaunch' is false" in {
          when(config.isDefaultFeatureFlagEnabled(refEq(Features.isPreLaunch))).thenReturn(true)
          authorizedUser()
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
        }
      }

    }

    "set expected page links" when {

      "group registration enabled" in {
        authorizedUser()
        verifyExpectedLinks(backLink = routes.RegistrationTypeController.displayPage().url,
                            changeLiabilityLink = startLiabilityLink.url
        )
      }

      "group registration enabled and group of organisation is selected" in {
        authorizedUser()

        val registration = aRegistration(withRegistrationType(Some(RegType.GROUP)))
        mockRegistrationFind(registration)
        given(page.apply(refEq(registration), any(), any())(any(), any())).willReturn(
          HtmlFormat.empty
        )
        when(mockStartRegistrationController.startLink(any())).thenReturn(startLiabilityLink)

        verifyExpectedLinks(backLink = routes.MembersUnderGroupControlController.displayPage().url,
                            changeLiabilityLink = startLiabilityLink.url
        )
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
      "user proceed" in {
        authorizedUser()

        val result = controller.submit()(postRequest(JsObject.empty))

        redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
      }
    }
  }

  private def verifyExpectedLinks(backLink: String, changeLiabilityLink: String): Unit = {
    val result = controller.displayPage()(getRequest())

    status(result) mustBe OK

    val backLinkCaptor: ArgumentCaptor[Call]            = ArgumentCaptor.forClass(classOf[Call])
    val changeLiabilityLinkCaptor: ArgumentCaptor[Call] = ArgumentCaptor.forClass(classOf[Call])

    verify(page).apply(any(), backLinkCaptor.capture(), changeLiabilityLinkCaptor.capture())(any(),
                                                                                             any()
    )

    backLinkCaptor.getValue.url mustBe backLink
    changeLiabilityLinkCaptor.getValue.url mustBe changeLiabilityLink
  }

}
