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

import base.MockAuthAction
import base.PptTestData.newUser
import org.bson.json.JsonObject
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, Result, Results}
import play.api.test.Helpers.{await, status}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import spec.PptTestData
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.UserEnrolmentDetails
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserDataRepository

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.parsing.json.JSONObject

class KeepAliveActionSpec
    extends MockAuthAction with AnyWordSpecLike with DefaultAwaitTimeout with BeforeAndAfterEach
    with PptTestData {

  private val mockUserDataRepository = mock[UserDataRepository]

  private val keepAliveAction: KeepAliveAction =
    new KeepAliveActionImpl(appConfig, mockUserDataRepository)(ExecutionContext.global)

  private val responseGenerator = mock[AuthenticatedRequest[_] => Future[Result]]

  private val requestCaptor: ArgumentCaptor[JourneyRequest[AnyContent]] =
    ArgumentCaptor.forClass(classOf[JourneyRequest[AnyContent]])

  when(responseGenerator.apply(requestCaptor.capture())).thenReturn(Future.successful(Results.Ok))

  "Keep Alive Action" should {

    "return a JourneyRequest populated with a cached registration" when {

      "a registration is cached on the user's session" in {
        val registration = aRegistration().copy(id = "3453456")
        val cachedRegistration: CacheItem =
          CacheItem("123", Json.toJsObject(registration), Instant.now(), Instant.now())
        when(mockUserDataRepository.findAll[JsValue](any())(any())).thenReturn(
          Future.successful(List(Json.toJson(registration)))
        )
        when(
          mockUserDataRepository.put[JsValue]("123")(DataKey("id"), Json.toJson(registration))
        ).thenReturn(Future.successful(cachedRegistration))
        val request =
          new AuthenticatedRequest(FakeRequest().withSession(("sessionId", "123")), newUser())

        status(keepAliveAction.invokeBlock(request, responseGenerator)) mustBe OK
      }

    }

    "throw errors" when {

      "no active session present" in {
        val request = new AuthenticatedRequest(FakeRequest(), newUser())

        intercept[SessionRecordNotFound] {
          keepAliveAction.invokeBlock(request, responseGenerator)
        }
      }

      "no record is returned when the key is not present" in {
        val registration = aRegistration().copy(id = "3453456")
        val cachedRegistration: CacheItem =
          CacheItem("123", Json.toJsObject(registration), Instant.now(), Instant.now())
        when(mockUserDataRepository.findAll[JsValue](any())(any())).thenReturn(
          Future.successful(List(Json.toJson("")))
        )
        when(
          mockUserDataRepository.put[JsValue]("123")(DataKey("id"), Json.toJson(registration))
        ).thenReturn(Future.successful(cachedRegistration))
        val request =
          new AuthenticatedRequest(FakeRequest().withSession(("sessionId", "123")), newUser())

        intercept[SessionRecordNotFound] {
          await(keepAliveAction.invokeBlock(request, responseGenerator))
        }
      }

    }
  }

}
