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

package uk.gov.hmrc.plasticpackagingtax.registration.models.request

import base.unit.ControllerSpec
import base.{MockAuthAction, PptTestData}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{Headers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, InternalServerException, RequestId}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration

import scala.concurrent.{ExecutionContext, Future}

class JourneyActionSpec extends ControllerSpec with MockAuthAction {

  private val responseGenerator = mock[JourneyRequest[_] => Future[Result]]

  private val actionRefiner = new JourneyAction(mockRegistrationConnector, mockAuditor, appConfig)(
    ExecutionContext.global
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRegistrationConnector, responseGenerator)
    given(responseGenerator.apply(any())).willReturn(Future.successful(Results.Ok))
  }

  "action refine" should {
    "permit request and send audit event" when {
      "internalId found" in {
        given(mockRegistrationConnector.find(refEq("123"))(any[HeaderCarrier])).willReturn(
          Future.successful(Right(Option(Registration("123"))))
        )
        val fakeRequest = FakeRequest().withSession("resumePPTRegistration" -> "true")

        await(
          actionRefiner.invokeBlock(authRequest(fakeRequest, user = PptTestData.newUser("123")),
                                    responseGenerator
          )
        ) mustBe Results.Ok

        verify(mockAuditor, never()).resumePPTRegistration(ArgumentMatchers.eq("123"))(any(), any())
      }
    }

    "pass through headers" when {
      "internalId found" in {
        val headers = Headers().add(HeaderNames.xRequestId -> "req1")
        given(mockRegistrationConnector.find(refEq("123"))(any[HeaderCarrier])).willReturn(
          Future.successful(Right(Option(Registration("123"))))
        )

        val fakeRequest =
          FakeRequest().withSession("resumePPTRegistration" -> "false").withHeaders(headers)

        await(
          actionRefiner.invokeBlock(authRequest(fakeRequest, user = PptTestData.newUser("123")),
                                    responseGenerator
          )
        ) mustBe Results.Ok

        getHeaders.requestId mustBe Some(RequestId("req1"))
        verify(mockAuditor).resumePPTRegistration(ArgumentMatchers.eq("123"))(any(), any())
      }
    }

    "create registration and send audit event" when {
      "registration details not found" in {
        given(mockRegistrationConnector.find(refEq("999"))(any[HeaderCarrier])).willReturn(
          Future.successful(Right(None))
        )
        given(
          mockRegistrationConnector.create(refEq(Registration("999")))(any[HeaderCarrier])
        ).willReturn(Future.successful(Right(Registration("999"))))

        await(
          actionRefiner.invokeBlock(authRequest(user = PptTestData.newUser("999")),
                                    responseGenerator
          )
        ) mustBe Results.Ok
        verify(mockAuditor).newRegistrationStarted(ArgumentMatchers.eq("999"))(any(), any())
      }
    }
  }

  def getHeaders: HeaderCarrier = {
    val captor = ArgumentCaptor.forClass(classOf[HeaderCarrier])
    verify(mockRegistrationConnector).find(refEq("123"))(captor.capture())
    captor.getValue
  }

  "throw exception" when {

    "cannot load user registration" in {
      given(mockRegistrationConnector.find(refEq("123"))(any[HeaderCarrier])).willReturn(
        Future.successful(
          Left(DownstreamServiceError("error", new InternalServerException("error")))
        )
      )

      intercept[DownstreamServiceError] {
        await(
          actionRefiner.invokeBlock(authRequest(user = PptTestData.newUser("123")),
                                    responseGenerator
          )
        )
      }
    }
  }
}
