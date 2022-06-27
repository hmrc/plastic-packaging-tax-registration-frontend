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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{LiabilityDetails, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.services.{TaxStartDate, TaxStartDateService}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.tax_start_date_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.LocalDate

class TaxStartDateControllerSpec extends ControllerSpec {

  private val page                    = mock[tax_start_date_page]
  private val mockTaxStartDateService = mock[TaxStartDateService]
  private val mcc                     = stubMessagesControllerComponents()
  private val calculateTaxStartDate   = LocalDate.of(2022, 4, 1)

  val sut = new TaxStartDateController(mockAuthAction,
                                       mockJourneyAction,
                                       mockTaxStartDateService,
                                       mcc,
                                       page,
                                       mockRegistrationConnector
  )

  override protected def beforeEach(): Unit = {
    reset(page, mockTaxStartDateService, mockRegistrationConnector)
    given(page.apply(any(), any())(any(), any())).willReturn(HtmlFormat.empty)
    given(mockTaxStartDateService.calculateTaxStartDate(any())).willReturn(Some(calculateTaxStartDate))
    given(mockTaxStartDateService.calculateTaxStartDate2(any())).willReturn(TaxStartDate.liableFrom(calculateTaxStartDate))
    mockRegistrationUpdate()
    super.beforeEach()
  }

  "Tax Start Date page " should {
    "return 200" when {
      "user is authorised and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())

        val result = sut.displayPage()(getRequest())

        status(result) mustBe OK
        verify(page).apply(startDate = ArgumentMatchers.eq(LocalDate.of(2022, 4, 1)), any())(
          any(),
          any()
        )
      }
    }

    "return an error" when {
      "user is not authorised and display page method is invoked" in {
        unAuthorizedUser()

        val result = sut.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "could not calculate date and display page method is invoked" ignore {
        // TODO possibly redundant test / replace it
        authorizedUser()
        when(mockTaxStartDateService.calculateTaxStartDate(any())).thenReturn(None)

        val result = sut.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    "calculate tax start date" when {
      "display page method is invoked" ignore {
        // TODO repair test once new tax start calculation in
        authorizedUser()
        val registration: Registration = aRegistration()
        mockRegistrationFind(registration)

        await(sut.displayPage()(getRequest()))

        verify(mockTaxStartDateService).calculateTaxStartDate(
          ArgumentMatchers.eq(registration.liabilityDetails)
        )
      }
    }

    "save the start date" when {
      "calculating the tax start date" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())

        val result = sut.displayPage()(getRequest())
        status(result) mustBe OK

        modifiedRegistration.liabilityDetails.startDate.get.asLocalDate mustBe calculateTaxStartDate
      }
    }

    "return the realised exceed threshold back link" when {
      "user expected to exceed the threshold and display page method is invoked" ignore {
        // TODO - redundant test? or needs updating?
        authorizedUser()
        mockRegistrationFind(aRegistration())

        await(sut.displayPage()(getRequest()))

        verify(page).apply(
          any(),
          any(),
        )(any(), any())
      }
    }

    "return the exceeded threshold link" when {
      "the threshold is breached and display page method is invoked" ignore {
        // TODO - redundant test? or needs updating?
        authorizedUser()

        mockRegistrationFind(
          aRegistration(
            withLiabilityDetails(LiabilityDetails(exceededThresholdWeight = Some(true)))
          )
        )

        await(sut.displayPage()(getRequest()))

        verify(page).apply(
          any(),
          any(),
        )(any(), any())
      }
    }

    "redirect to Capture Weight page" when {
      "submit" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())

        val result = sut.submit()(postRequest(JsObject.empty))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.LiabilityWeightController.displayPage().url)
      }
    }
  }
}
