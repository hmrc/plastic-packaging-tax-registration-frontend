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

package controllers.liability

import base.unit.ControllerSpec
import controllers.{routes => pptRoutes}
import forms.liability.RegType
import models.registration.NewLiability
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.`given`
import org.mockito.MockitoSugar.{reset, verify}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.{TaxStartDate, TaxStartDateService}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.liability.check_liability_details_answers_page

import java.time.LocalDate
import scala.concurrent.Future

class CheckLiabilityDetailsAnswersControllerTest extends ControllerSpec {
  private val page                    = mock[check_liability_details_answers_page]
  private val mcc                     = stubMessagesControllerComponents()
  private val mockTaxStartDateService = mock[TaxStartDateService]
  private val aDate                   = LocalDate.of(2022, 4, 1)

  private val controller =
    new CheckLiabilityDetailsAnswersController(
      journeyAction = spyJourneyAction,
      mcc = mcc,
      mockRegistrationConnector,
      mockTaxStartDateService,
      page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(page, mockTaxStartDateService, mockRegistrationConnector, config)

    val registration =
      aRegistration(
        withRegistrationType(Some(RegType.SINGLE_ENTITY)),
        r => r.copy(liabilityDetails = r.liabilityDetails.copy(newLiabilityFinished = Some(NewLiability)))
      )
    spyJourneyAction.setReg(registration)
    given(page.apply(refEq(registration))(any(), any())).willReturn(HtmlFormat.empty)
    given(mockTaxStartDateService.calculateTaxStartDate(any())).willReturn(TaxStartDate.liableFromBackwardsTest(aDate))
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

          val registration = aRegistration()
          spyJourneyAction.setReg(registration)

          val result: Future[Result] = controller.displayPage()(FakeRequest())
          status(result) shouldEqual Status.OK

          verify(mockTaxStartDateService).calculateTaxStartDate(ArgumentMatchers.eq(registration.liabilityDetails))
        }
      }
    }

    "set expected page links" when {

      "check the backward look feature flags" in {

        given(page.apply(any())(any(), any())).willReturn(HtmlFormat.empty)

        await(controller.displayPage()(FakeRequest()))

        verify(page).apply(any())(any(), any())
      }

      "group registration enabled and group of organisation is selected" in {

        val registration =
          aRegistration(
            withRegistrationType(Some(RegType.GROUP)),
            r => r.copy(liabilityDetails = r.liabilityDetails.copy(newLiabilityFinished = Some(NewLiability)))
          )
        spyJourneyAction.setReg(registration)
        given(page.apply(refEq(registration))(any(), any())).willReturn(HtmlFormat.empty)

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
        verify(page).apply(any())(any(), any())
      }
    }

    "updates liability with the correct start date and redirects to registration page" when {
      "user proceed" in {

        val registration = aRegistration()
        spyJourneyAction.setReg(registration)

        val result = controller.submit()(postRequest(Json.toJson(registration)))

        redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)

        verify(mockTaxStartDateService).calculateTaxStartDate(ArgumentMatchers.eq(registration.liabilityDetails))

        modifiedRegistration.liabilityDetails.startDate.get.asLocalDate mustBe aDate
      }
    }
  }
}
