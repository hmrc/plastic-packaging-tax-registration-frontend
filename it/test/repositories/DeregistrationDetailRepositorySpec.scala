/*
 * Copyright 2024 HM Revenue & Customs
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

package test.repositories

import models.deregistration.{DeregistrationDetails, DeregistrationReason}
import models.request.AuthenticatedRequest
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.mvc.AnyContent
import play.api.test.DefaultAwaitTimeout
import repositories.{DeregistrationDetailRepositoryImpl, MongoUserDataRepository}
import spec.PptTestData
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class DeregistrationDetailRepositorySpec
    extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach with DefaultAwaitTimeout with MongoSupport with PptTestData {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))

  override implicit val authenticatedRequest: AuthenticatedRequest[AnyContent] = registrationRequest

  private val mockConfig = mock[Configuration]
  when(mockConfig.get[FiniteDuration]("mongodb.userDataCache.expiry")).thenReturn(FiniteDuration(1, TimeUnit.MINUTES))

  val mockTimeStampSupport = new CurrentTimestampSupport()

  val userDataRepository =
    new MongoUserDataRepository(mongoComponent, mockConfig, mockTimeStampSupport)

  val deregistrationDetailRepository = new DeregistrationDetailRepositoryImpl(userDataRepository)

  implicit val authRequest: AuthenticatedRequest[Any] = registrationRequest

  private val deregistrationDetail =
    DeregistrationDetails(Some(true), Some(DeregistrationReason.RegisteredIncorrectly))

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropDatabase()
  }

  "Deregistration Detail Repository" should {

    "return empty DeregistrationDetails when no previously stored deregistration detail" in {
      deregistrationDetailRepository.get().map(_ mustBe DeregistrationDetails(None, None))
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
            storedRegistrationDetail.reason mustBe Some(DeregistrationReason.CeasedTrading)
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
  }
}
