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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.PptTestData.newUser
import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, MongoCacheRepository}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.MongoUserDataRepository
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.time.Instant
import scala.concurrent.Future

class KeepAliveControllerSpec extends ControllerSpec {
  private val mcc                    = stubMessagesControllerComponents()
  private val mockUserDataRepository = mock[MongoUserDataRepository]

  private val controller =
    new KeepAliveController(authenticate = mockAuthAllowEnrolmentAction,
                            userDataRepository = mockUserDataRepository,
                            mcc = mcc
    )

  override protected def afterEach(): Unit =
    super.afterEach()

  "Keepalive controller" should {

    "return 200 and keep all documents alive" when {

      "user is authorised and display page method is invoked" in {
        implicit val cif = MongoCacheRepository.format
        val registration: (String, JsValue) =
          "registrationAmendment" -> Json.toJson(aRegistration())
        val cachedRegistration: CacheItem =
          CacheItem("sessionId", JsObject.apply(Map(registration)), Instant.now(), Instant.now())
        when(mockUserDataRepository.findBySessionId(any())).thenReturn(
          Future.successful(Some(cachedRegistration))
        )
        authorizedUser()
        val result = controller.keepAlive()(
          new AuthenticatedRequest(FakeRequest().withSession(("sessionId", "123")), newUser())
        )

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

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.keepAlive()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }
  }

}
