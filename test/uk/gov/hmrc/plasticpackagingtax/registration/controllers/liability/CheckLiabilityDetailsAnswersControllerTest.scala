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
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Call, Result}
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{StartRegistrationController, routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.services.{TaxStartDate, TaxStartDateService}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.check_liability_details_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.LocalDate
import scala.concurrent.Future

class CheckLiabilityDetailsAnswersControllerTest extends ControllerSpec {
  private val page                            = mock[check_liability_details_answers_page]
  private val mcc                             = stubMessagesControllerComponents()
  private val mockStartRegistrationController = mock[StartRegistrationController]
  private val mockTaxStartDateService         = mock[TaxStartDateService]
  private val aDate                           = LocalDate.of(2022, 4, 1)

  private val startLiabilityLink = Call("GET", "/start-liability")

  private val controller =
    new CheckLiabilityDetailsAnswersController(authenticate = mockAuthAction,
      mockJourneyAction,
      mcc = mcc,
      mockRegistrationConnector,
      mockTaxStartDateService,
      page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(page, mockTaxStartDateService, mockRegistrationConnector)
    val registration = aRegistration(withRegistrationType(Some(RegType.SINGLE_ENTITY)))
    mockRegistrationFind(registration)
    given(page.apply(refEq(registration), any())(any(), any())).willReturn(HtmlFormat.empty)
    given(mockTaxStartDateService.calculateTaxStartDate(any())).willReturn(TaxStartDate.liableFromBackwardsTest(aDate))
    when(mockStartRegistrationController.startLink(any())).thenReturn(startLiabilityLink)
    mockRegistrationUpdate()
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Check liability details answers Controller" should {

    "displayPage" when {

      "user is authorised" should {
        "return 200" in {
          authorizedUser()

          val registration = aRegistration()
          mockRegistrationFind(registration)

          val result: Future[Result] = controller.displayPage()(getRequest())
          status(result) shouldEqual Status.OK

          verify(mockTaxStartDateService).calculateTaxStartDate(
            ArgumentMatchers.eq(registration.liabilityDetails)
          )
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
        given(page.apply(refEq(registration), any())(any(), any())).willReturn(HtmlFormat.empty)
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

    "updates liability with the correct start date and redirects to registration page" when {
      "user proceed" in {
        authorizedUser()

        val registration = aRegistration()
        mockRegistrationFind(registration)

        val result = controller.submit()(postRequest(Json.toJson(registration)))

        redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)

        verify(mockTaxStartDateService).calculateTaxStartDate(
          ArgumentMatchers.eq(registration.liabilityDetails)
        )

        modifiedRegistration.liabilityDetails.startDate.get.asLocalDate mustBe aDate
      }
    }
  }

  private def verifyExpectedLinks(backLink: String, changeLiabilityLink: String): Unit = {
    val result = controller.displayPage()(getRequest())

    status(result) mustBe OK

    val backLinkCaptor: ArgumentCaptor[Call] = ArgumentCaptor.forClass(classOf[Call])

    verify(page).apply(any(), backLinkCaptor.capture())(any(), any())

    backLinkCaptor.getValue.url mustBe backLink
  }

}
