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

package controllers

import base.PptTestData.newUser
import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import uk.gov.hmrc.auth.core.SessionRecordNotFound
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey}
import models.request.AuthenticatedRequest
import repositories.MongoUserDataRepository
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.Instant
import scala.concurrent.Future

class KeepAliveControllerSpec extends ControllerSpec {
  private val mcc                    = stubMessagesControllerComponents()
  private val mockUserDataRepository = mock[MongoUserDataRepository]

  private val controller =
    new KeepAliveController(authenticate = FakeBasicAuthAction,
                            userDataRepository = mockUserDataRepository,
                            mcc = mcc
    )

  override protected def afterEach(): Unit =
    super.afterEach()

  "Keepalive controller" should {

    "return 200 and keep all documents alive" when {

      "user is authorised and display page method is invoked" in {
        val registration: (String, JsValue) =
          "registrationAmendment" -> Json.toJson(aRegistration())
        val cachedRegistration: CacheItem =
          CacheItem("sessionId", JsObject.apply(Map(registration)), Instant.now(), Instant.now())
        when(mockUserDataRepository.findBySessionId(any())).thenReturn(
          Future.successful(cachedRegistration)
        )

        val result = controller.keepAlive()(registrationJourneyRequest)

        status(result) mustBe OK
        val cacheItemCaptor: ArgumentCaptor[CacheItem] =
          ArgumentCaptor.forClass(classOf[CacheItem])
        verify(mockUserDataRepository).put(any[String])(
          DataKey(ArgumentMatchers.eq("registrationAmendment")),
          cacheItemCaptor.capture()
        )(any())
        cacheItemCaptor.getValue mustBe Json.toJson(cachedRegistration.data.fields.head._2)
      }
    }

    "return an error" when {

      "no record is found for the sessionId" in {

        when(mockUserDataRepository.findBySessionId("123456")).thenReturn(
          Future.failed(SessionRecordNotFound())
        )
        val result =
          controller.keepAlive()(registrationJourneyRequest)

        status(result) mustBe OK
        verify(mockUserDataRepository, never()).put(any[String])(
          DataKey(ArgumentMatchers.eq("123")),
          any()
        )(any())
      }
      "no value in the record for the sessionId" in {

        val cachedRegistration: CacheItem =
          CacheItem("sessionId", JsObject.empty, Instant.now(), Instant.now())
        when(mockUserDataRepository.findBySessionId(any())).thenReturn(
          Future.successful(cachedRegistration)
        )
        val result =
          controller.keepAlive()(registrationJourneyRequest)

        status(result) mustBe OK
        verify(mockUserDataRepository, never()).put(any[String])(
          DataKey(ArgumentMatchers.eq("123")),
          any()
        )(any())
      }
      "no value for sessionId" in {

        val result =
          controller.keepAlive()(registrationJourneyRequest)

        intercept[RuntimeException](status(result))
      }
    }
  }

}
