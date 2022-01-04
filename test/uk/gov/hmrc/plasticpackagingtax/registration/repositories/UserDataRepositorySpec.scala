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

package uk.gov.hmrc.plasticpackagingtax.registration.repositories

import base.PptTestData
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.test.Helpers.await
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class UserDataRepositorySpec
    extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach
    with DefaultAwaitTimeout with MongoSupport {

  val mockConfig = mock[Configuration]
  when(mockConfig.get[FiniteDuration]("mongodb.userDataCache.expiry")).thenReturn(
    FiniteDuration(1, TimeUnit.MINUTES)
  )

  val mockTimeStampSupport = new CurrentTimestampSupport()

  val repository = new UserDataRepository(mongoComponent, mockConfig, mockTimeStampSupport)

  def authRequest(sessionId: String): AuthenticatedRequest[Any] =
    new AuthenticatedRequest(FakeRequest().withSession("sessionId" -> sessionId),
                             PptTestData.newUser("123")
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropDatabase()
  }

  "UserDataRepository" should {

    "add data to cache and return it" when {
      "explicit id used" in {
        val id = "123"
        await(repository.putData(id, "testKey", "testData"))

        await(repository.getData[String](id, "testKey")) mustBe Some("testData")
      }
      "user's session id used as id" in {
        implicit val request: AuthenticatedRequest[Any] = authRequest("12345")
        await(repository.putData("testKey", "testData"))

        await(repository.getData[String]("testKey")) mustBe Some("testData")
      }
    }

    "return None when no data found" in {

      implicit val request: AuthenticatedRequest[Any] = authRequest("12345")

      await(repository.getData[String]("some-key")) mustBe None
    }

    "add data to cache and delete it" when {
      "explicit id used" in {
        val id                                          = "123"
        implicit val request: AuthenticatedRequest[Any] = authRequest("12345")
        await(repository.putData(id, "testKey", "testData"))

        await(repository.deleteData[String](id, "testKey")).mustBe(())
      }
      "user's session id used as id" in {
        implicit val request: AuthenticatedRequest[Any] = authRequest("12345")
        await(repository.putData("testKey", "testData"))

        await(repository.deleteData[String]("testKey")).mustBe(())
      }
    }
  }
}
