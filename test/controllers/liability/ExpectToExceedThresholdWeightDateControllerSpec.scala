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

package controllers.liability

import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import forms.Date
import forms.liability.ExpectToExceedThresholdWeightDate
import models.registration.{LiabilityDetails, NewLiability, Registration}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.liability.expect_to_exceed_threshold_weight_date_page

import java.time.LocalDate

class ExpectToExceedThresholdWeightDateControllerSpec extends ControllerSpec with BeforeAndAfterEach {

  private val page: expect_to_exceed_threshold_weight_date_page = mock[expect_to_exceed_threshold_weight_date_page]
  private val mockFormProvider = mock[ExpectToExceedThresholdWeightDate]
  private val form = mock[Form[LocalDate]]
  private val mcc: MessagesControllerComponents = stubMessagesControllerComponents()

  private val sut = new ExpectToExceedThresholdWeightDateController(
    journeyAction = spyJourneyAction,
    mockRegistrationConnector,
    mcc,
    page,
    mockFormProvider,
    config
  )

  override def beforeEach() = {
    super.beforeEach()

    reset(mockFormProvider, page, form)

    when(page.apply(any())(any(),any())).thenReturn(HtmlFormat.empty)
  }
  "onPageLoad" should {

    "return 200" in {


        spyJourneyAction.setReg(aRegistration())
        when(mockFormProvider.apply()(any())).thenReturn(form)

        val result = sut.displayPage()()(getRequest())

        status(result) mustBe OK
    }

    "display a view with empty date" in {

      val registration =
        aRegistration(withLiabilityDetails(LiabilityDetails(dateRealisedExpectedToExceedThresholdWeight = None)))

      spyJourneyAction.setReg(registration)
      when(mockFormProvider.apply()(any())).thenReturn(form)

      await(sut.displayPage()(getRequest()))

      verify(page).apply(ArgumentMatchers.eq(form))(any(),any())
      verifyNoInteractions(form)
    }

    "display a view with a date" in {

      spyJourneyAction.setReg(aRegistration())
      when(mockFormProvider.apply()(any())).thenReturn(form)
      when(form.fill(any())).thenReturn(form)

      await(sut.displayPage()(getRequest()))

      verify(form).fill(LocalDate.of(2022,3,5))
      verify(page).apply(ArgumentMatchers.eq(form))(any(),any())
    }

    "return Unauthorised" in {


      val result = sut.displayPage()(getRequest())

      intercept[RuntimeException](status(result))
    }

  }

  "submit" should {
    "redirect" in {
      setUpMockForSubmit()

      val result = sut.submit()(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        routes.ExceededThresholdWeightController.displayPage().url
      )
    }

    "save the date to cache" in {
      setUpMockForSubmit()
      val reg = aRegistration()
      spyJourneyAction.setReg(reg)
      when(config.isBackwardLookChangeEnabled).thenReturn(false)

      await(sut.submit()(FakeRequest()))

      verify(mockRegistrationConnector).update(ArgumentMatchers.eq(createExpectedRegistration(reg)))(any())
    }

    "error" when {
      "date is not valid" in {
        val bindForm = createForm.withError("error", "error message")
        setUpMockForSubmit(bindForm)

        val result = sut.submit()(FakeRequest())

        status(result) mustBe BAD_REQUEST
        verify(page).apply(ArgumentMatchers.eq(bindForm))(any(),any())
      }

      "saving to cache" in {
        setUpMockForSubmit()
        mockRegistrationUpdateFailure()

        val result = sut.submit()(FakeRequest())

        intercept[DownstreamServiceError](status(result))
      }
    }
  }

  private def setUpMockForSubmit(bindForm: Form[LocalDate] = createForm): Unit = {

    when(mockFormProvider.apply()(any())).thenReturn(form)
    when(form.bindFromRequest()(any(), any())).thenReturn(bindForm)
    mockRegistrationUpdate()
  }

  private def createExpectedRegistration(reg: Registration) = {
    val updatedLiableDetails =
      reg.liabilityDetails.copy(
        dateRealisedExpectedToExceedThresholdWeight = Some(Date(LocalDate.of(2022, 4, 1))),
        newLiabilityStarted = Some(NewLiability)
      )

    reg.copy(liabilityDetails = updatedLiableDetails)
  }

  private def createForm: Form[LocalDate] =  {
    new ExpectToExceedThresholdWeightDate(config).apply()(mock[Messages])
      .fill(LocalDate.of(2022,4,1))
  }

}
