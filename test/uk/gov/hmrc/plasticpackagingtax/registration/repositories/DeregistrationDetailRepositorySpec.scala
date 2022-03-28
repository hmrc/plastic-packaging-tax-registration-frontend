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
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.models.deregistration.{
  DeregistrationDetails,
  DeregistrationReason
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class DeregistrationDetailRepositorySpec
    extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach
    with DefaultAwaitTimeout with MongoSupport {

  private val appConfig  = mock[AppConfig]
  private val mockConfig = mock[Configuration]
  when(mockConfig.get[FiniteDuration]("mongodb.userDataCache.expiry")).thenReturn(
    FiniteDuration(1, TimeUnit.MINUTES)
  )

  val mockTimeStampSupport = new CurrentTimestampSupport()

  val userDataRepository =
    new MongoUserDataRepository(mongoComponent, mockConfig, mockTimeStampSupport)

  val deregistrationDetailRepository = new DeregistrationDetailRepositoryImpl(userDataRepository)

  implicit val request: AuthenticatedRequest[Any] = authRequest("12345")

  private def authRequest(sessionId: String): AuthenticatedRequest[Any] =
    new AuthenticatedRequest(FakeRequest().withSession("sessionId" -> sessionId),
                             PptTestData.newUser("123"),
                             appConfig
    )

  private val deregistrationDetail =
    DeregistrationDetails(Some(true), Some(DeregistrationReason.RegisteredIncorrectly))

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropDatabase()
  }

  "Deregistration Detail Repository" should {

    "return None when no previously stored deregistration detail" in {
      deregistrationDetailRepository.get().map(_ mustBe None)
    }

    "persist deregistration detail" in {
      deregistrationDetailRepository.put(deregistrationDetail).map { _ =>
        deregistrationDetailRepository.get().map(_ mustBe deregistrationDetail)
      }
    }

    "update deregistration detail" in {
      deregistrationDetailRepository.put(deregistrationDetail).map { _ =>
        deregistrationDetailRepository.update { storedDeregistrationDetail =>
          storedDeregistrationDetail.copy(reason = Some(DeregistrationReason.CeasedTrading))
        }.map { _ =>
          deregistrationDetailRepository.get().map { storedRegistrationDetail =>
            storedRegistrationDetail.get.reason mustBe Some(DeregistrationReason.CeasedTrading)
          }
        }
      }
    }

    "delete deregistration detail" in {
      deregistrationDetailRepository.put(deregistrationDetail).map { _ =>
        deregistrationDetailRepository.delete().map { _ =>
          deregistrationDetailRepository.get() mustBe None
        }
      }
    }

    "throw IllegalStateException if attempt to update missing deregistration detail" in {
      intercept[IllegalStateException] {
        await(deregistrationDetailRepository.update(dd => dd))
      }
    }
  }
}
