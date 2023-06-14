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

package repositories

import builders.RegistrationBuilder
import models.registration.Registration
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.mockito.invocation.InvocationOnMock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.await
import spec.PptTestData

import scala.concurrent.{ExecutionContext, Future}

class MongoRegistrationAmendmentRepositorySpec
    extends AnyWordSpec with RegistrationBuilder with Matchers with MockitoSugar
    with BeforeAndAfterEach with DefaultAwaitTimeout with PptTestData {

  private val mockUserDataRepository = mock[UserDataRepository]

  private val mongoRegistrationAmendmentRepository = new RegistrationAmendmentRepositoryImpl(
    mockUserDataRepository
  )(ExecutionContext.global)

  private val sessionId        = "123"
  private val registration     = aRegistration()
  private implicit val authRequest = registrationRequest

  override protected def beforeEach(): Unit = {
    reset(mockUserDataRepository)
    when(mockUserDataRepository.putData[Registration](any(), any())(any(), any())).thenAnswer(
      (inv: InvocationOnMock) => Future.successful(inv.getArgument(0))
    )
    when(mockUserDataRepository.getData[Registration](any())(any(), any())).thenReturn(
      Future.successful(Some(registration))
    )
    when(mockUserDataRepository.getData[Registration](any(), any())(any())).thenReturn(
      Future.successful(Some(registration))
    )
  }

  "Mongo Registration Amendment Repository" should {
    "invoke underlying User Data Repository as expected" when {

      "adding registration" in {
        mongoRegistrationAmendmentRepository.put(registration)
        verify(mockUserDataRepository).putData(RegistrationAmendmentRepositoryImpl.repositoryKey,
                                               registration
        )

        mongoRegistrationAmendmentRepository.put(sessionId, registration)
        verify(mockUserDataRepository).putData(sessionId,
                                               RegistrationAmendmentRepositoryImpl.repositoryKey,
                                               registration
        )
      }

      "getting registration" in {
        await(mongoRegistrationAmendmentRepository.get()) mustBe Some(registration)
        await(mongoRegistrationAmendmentRepository.get(sessionId)) mustBe Some(registration)
      }

      "updating a registration" in {
        val updatedRegistration = registration.copy(id = "08345982374")
        await(mongoRegistrationAmendmentRepository.update(_ => updatedRegistration))
        verify(mockUserDataRepository).putData(RegistrationAmendmentRepositoryImpl.repositoryKey,
                                               updatedRegistration
        )
      }
    }

    "throw exception when attempt to update a registration which does not exist" in {
      when(mockUserDataRepository.getData[Registration](any())(any(), any())).thenReturn(
        Future.successful(None)
      )
      when(mockUserDataRepository.getData[Registration](any(), any())(any())).thenReturn(
        Future.successful(None)
      )
      intercept[IllegalStateException] {
        await(mongoRegistrationAmendmentRepository.update(reg => reg))
      }
    }
  }
}
