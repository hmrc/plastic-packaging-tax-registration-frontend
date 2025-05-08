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

package controllers.deregistration

import base.unit.{AmendmentControllerSpec, ControllerSpec}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import connectors.{DeregistrationConnector, DownstreamServiceError, FailedToDeregister}
import models.deregistration.{DeregistrationDetails, DeregistrationReason}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, verify, when}
import play.api.test.FakeRequest
import repositories.{DeregistrationDetailRepositoryImpl, UserDataRepository}
import views.html.deregistration.deregister_check_your_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class DeregisterCheckYourAnswersControllerSpec extends ControllerSpec with AmendmentControllerSpec {
  private val page                      = mock[deregister_check_your_answers_page]
  private val mcc                       = stubMessagesControllerComponents()
  private val mockCache                 = mock[UserDataRepository]
  private val repository                = new DeregistrationDetailRepositoryImpl(mockCache)
  private val mockDegistrationConnector = mock[DeregistrationConnector]

  private val initialDeregistrationDetails =
    DeregistrationDetails(deregister = Some(true), reason = Some(DeregistrationReason.RegisteredIncorrectly))

  private val controller =
    new DeregisterCheckYourAnswersController(
      authenticate = FakeAmendAuthAction,
      mcc = mcc,
      deregistrationDetailRepository = repository,
      deregistrationConnector = mockDegistrationConnector,
      page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[DeregistrationDetails])(any, any)).thenReturn(HtmlFormat.empty)
    when(mockCache.getData[DeregistrationDetails](any)(any, any)).thenReturn(
      Future.successful(Some(initialDeregistrationDetails))
    )
  }

  override protected def afterEach(): Unit = {
    reset(page, mockCache)
    super.afterEach()
  }

  "DeregisterationCheckYourAnswers Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "submit the answers" in {
        when(
          mockDegistrationConnector.deregister(
            ArgumentMatchers.eq("XMPPT0000000123"),
            ArgumentMatchers.eq(initialDeregistrationDetails)
          )(any)
        ).thenReturn(
          Future.successful(Right(()))
        )

        val result: Future[Result] = controller.continue()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DeregistrationSubmittedController.displayPage().url)

        verify(mockCache).deleteData[DeregistrationDetails](any)(any, any)

      }

      "when error in the response status" in {
        when(mockDegistrationConnector.deregister(any, any)(any)).thenReturn(
          Future.successful(Left(DownstreamServiceError("failed", new FailedToDeregister("failed to de-register"))))
        )

        intercept[DownstreamServiceError](status(controller.continue()(request)))
      }

    }

  }
}
