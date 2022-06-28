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
import org.mockito.ArgumentMatchers.{any, eq => eqq}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, verify}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.JsObject
import play.api.mvc.Results.{Ok, Redirect}
import play.api.test.Helpers.{await, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.services.{TaxStartDate, TaxStartDateService}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.tax_start_date_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.LocalDate

class TaxStartDateControllerSpec extends ControllerSpec {

  private val page = mock[tax_start_date_page]
  private val mockTaxStartDateService = mock[TaxStartDateService]
  private val mcc = stubMessagesControllerComponents()
  private val aDate = LocalDate.of(2022, 4, 1)
  private val aRegistration = super.aRegistration()

  val sut = new TaxStartDateController(
    mockAuthAction,
    mockJourneyAction,
    mockTaxStartDateService,
    mcc,
    page,
    mockRegistrationConnector
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(page, mockTaxStartDateService, mockRegistrationConnector)
    authoriseAndSetRegistration
  }

  private def authoriseAndSetRegistration = {
    authorizedUser()
    mockRegistrationFind(aRegistration())
  }

  "display page" should {
    
    "pass the liability answers to the tax start date service" in {
      given(mockTaxStartDateService.calculateTaxStartDate(any())).willReturn(TaxStartDate.notLiable)
      await(sut.displayPage()(getRequest()))
      verify(mockTaxStartDateService).calculateTaxStartDate(eqq(aRegistration.liabilityDetails))
    }

    "bounce to the not liable page" in {
      given(mockTaxStartDateService.calculateTaxStartDate(any())).willReturn(TaxStartDate.notLiable)
      val result = await(sut.displayPage()(getRequest()))
      result mustBe Redirect(routes.NotLiableController.displayPage())
    }

    "display tax start date page" in {
      given(mockTaxStartDateService.calculateTaxStartDate(any())).willReturn(TaxStartDate.liableFromForwardsTest(aDate))
      given(page.apply(any(), any())(any(), any())).willReturn(HtmlFormat.raw("tax start date blah"))
      val result = await(sut.displayPage()(getRequest()))
      verify(page).apply(eqq(aDate), eqq(false))(any(), any())
      result mustBe Ok(HtmlFormat.raw("tax start date blah"))
    }

    "return an error" when {
      "user is not authorised and display page method is invoked" in {
        unAuthorizedUser()
        val result = sut.displayPage()(getRequest())
        intercept[RuntimeException](status(result))
      }
    }
    
  }

  "submit" should {
    "redirect to the next page" in {
      val result = await(sut.submit()(postRequest(JsObject.empty)))
      result mustBe Redirect(routes.LiabilityWeightController.displayPage())
    }
  }

}
