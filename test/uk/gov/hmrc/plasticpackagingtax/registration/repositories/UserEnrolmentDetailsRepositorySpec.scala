/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.concurrent.TimeUnit
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
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.{IsUkAddress, PptReference}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.UserEnrolmentDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class UserEnrolmentDetailsRepositorySpec
    extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach
    with DefaultAwaitTimeout with MongoSupport {

  val mockConfig = mock[Configuration]
  when(mockConfig.get[FiniteDuration]("mongodb.userDataCache.expiry")).thenReturn(
    FiniteDuration(1, TimeUnit.MINUTES)
  )

  val mockTimeStampSupport = new CurrentTimestampSupport()

  val userDataRepository             = new UserDataRepository(mongoComponent, mockConfig, mockTimeStampSupport)
  val userEnrolmentDetailsRepository = new UserEnrolmentDetailsRepository(userDataRepository)

  implicit val request: AuthenticatedRequest[Any] = authRequest("12345")

  def authRequest(sessionId: String): AuthenticatedRequest[Any] =
    new AuthenticatedRequest(FakeRequest().withSession("sessionId" -> sessionId),
                             PptTestData.newUser("123")
    )

  val userEnrolmentDetails =
    UserEnrolmentDetails(pptReference = Some(PptReference("ppt-ref")),
                         isUkAddress = Some(IsUkAddress(Some(true))),
                         postcode = Some("postcode"),
                         registrationDate = Some(Date(Some(1), Some(2), Some(2022)))
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    dropDatabase()
  }

  "PublicBodyRegistrationRepository" should {

    "add data to cache and return it" in {

      await(userEnrolmentDetailsRepository.put(userEnrolmentDetails))

      await(userEnrolmentDetailsRepository.get()) mustBe Some(userEnrolmentDetails)
    }

    "update data in the cache" when {

      "a registration exists" in {

        await(userEnrolmentDetailsRepository.put(userEnrolmentDetails))

        await(
          userEnrolmentDetailsRepository.update(
            reg => reg.copy(pptReference = Some(PptReference("update-reference")))
          )
        )

        await(userEnrolmentDetailsRepository.get()) mustBe Some(
          userEnrolmentDetails.copy(pptReference = Some(PptReference("update-reference")))
        )
      }

      "a registration does not exists" in {

        await(
          userEnrolmentDetailsRepository.update(
            reg => reg.copy(pptReference = Some(PptReference("new-reference")))
          )
        )

        await(userEnrolmentDetailsRepository.get()) mustBe Some(
          UserEnrolmentDetails(Some(PptReference("new-reference")), None, None)
        )
      }

    }

    "return None when no data found" in {

      await(userEnrolmentDetailsRepository.get()) mustBe None
    }

  }
}
