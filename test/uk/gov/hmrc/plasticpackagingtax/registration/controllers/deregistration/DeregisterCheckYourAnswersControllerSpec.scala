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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.deregistration

import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{
  DeregistrationConnector,
  DownstreamServiceError,
  FailedToDeregister
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.deregistration.{
  DeregistrationDetails,
  DeregistrationReason
}
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.{
  DeregistrationDetailRepositoryImpl,
  UserDataRepository
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.deregistration.deregister_check_your_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class DeregisterCheckYourAnswersControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction {
  private val page                      = mock[deregister_check_your_answers_page]
  private val mcc                       = stubMessagesControllerComponents()
  private val mockCache                 = mock[UserDataRepository]
  private val repository                = new DeregistrationDetailRepositoryImpl(mockCache)
  private val mockDegistrationConnector = mock[DeregistrationConnector]

  private val initialDeregistrationDetails =
    DeregistrationDetails(deregister = Some(true),
                          reason = Some(DeregistrationReason.RegisteredIncorrectly)
    )

  private val controller =
    new DeregisterCheckYourAnswersController(authenticate = mockAuthAllowEnrolmentAction,
                                             mcc = mcc,
                                             deregistrationDetailRepository = repository,
                                             deregistrationConnector = mockDegistrationConnector,
                                             page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[DeregistrationDetails])(any(), any())).thenReturn(HtmlFormat.empty)
    when(mockCache.getData[DeregistrationDetails](any())(any(), any())).thenReturn(
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
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "submit the answers" in {
        authorisedUserWithPptSubscription()
        when(
          mockDegistrationConnector.deregister(ArgumentMatchers.eq("XMPPT0000000123"),
                                               ArgumentMatchers.eq(initialDeregistrationDetails)
          )(any())
        ).thenReturn(Future.successful(Right(HttpResponse.apply(Status.OK, ""))))
        val result: Future[Result] = controller.continue()(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.DeregistrationSubmittedController.displayPage().url
        )

        verify(mockCache).deleteData[DeregistrationDetails](any())(any(), any())

      }

      "when error in the response status" in {
        authorisedUserWithPptSubscription()
        when(mockDegistrationConnector.deregister(any(), any())(any())).thenReturn(
          Future.successful(
            Left(DownstreamServiceError("failed", new FailedToDeregister("failed to de-register")))
          )
        )
        val result = controller.continue()(request)

        intercept[DownstreamServiceError](status(result))
      }

      "throw a RuntimeException" when {
        "user is not authorised" in {
          unAuthorizedUser()
          val result = controller.displayPage()(getRequest())

          intercept[RuntimeException](status(result))
        }
      }

    }

  }
}
