/*
 * Copyright 2025 HM Revenue & Customs
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

import forms.enrolment._
import models.registration.UserEnrolmentDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UserEnrolmentDetailsRepository.repositoryKey
import repositories.{UserDataRepository, UserEnrolmentDetailsRepository}
import spec.PptTestData

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class UserEnrolmentDetailsRepositorySpec extends PlaySpec with BeforeAndAfterEach with PptTestData {

  val mockUserDataRepository: UserDataRepository = mock[UserDataRepository]

  val sut = new UserEnrolmentDetailsRepository(mockUserDataRepository)(global)

  override val userEnrolmentDetails =
    UserEnrolmentDetails(
      pptReference = Some(PptReference("ppt-ref")),
      isUkAddress = Some(IsUkAddress(Some(true))),
      postcode = Some(Postcode("LS1 1AA")),
      registrationDate = Some(RegistrationDate(DateData("1", "2", "2022")))
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUserDataRepository)
  }

  "User Enrolment Details Repository" must {

    "get" when {
      "there is one" in {
        when(mockUserDataRepository.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(Future.successful(Some(userEnrolmentDetails)))
        val result = await(sut.get()(authenticatedRequest))

        verify(mockUserDataRepository).getData(repositoryKey)(UserEnrolmentDetails.format, authenticatedRequest)
        result mustBe userEnrolmentDetails
      }
      "there is NOT one" in {
        when(mockUserDataRepository.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(Future.successful(None))
        val result = await(sut.get()(authenticatedRequest))

        verify(mockUserDataRepository).getData(repositoryKey)(UserEnrolmentDetails.format, authenticatedRequest)
        result mustBe UserEnrolmentDetails()
      }
    }
    "put" in {
      when(mockUserDataRepository.putData[Any](any(), any())(any(), any())).thenReturn(Future.successful(userEnrolmentDetails))
      val result = await(sut.put(userEnrolmentDetails)(authenticatedRequest))

      verify(mockUserDataRepository)
        .putData(repositoryKey, userEnrolmentDetails)(UserEnrolmentDetails.format, authenticatedRequest)
      result mustBe userEnrolmentDetails
    }

    "update" in {
      val updateFunc = mock[UserEnrolmentDetails => UserEnrolmentDetails]
      when(updateFunc.apply(any())).thenReturn(userEnrolmentDetails)
      when(mockUserDataRepository.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(Future.successful(Some(userEnrolmentDetails)))
      when(mockUserDataRepository.putData[Any](any(), any())(any(), any())).thenReturn(Future.successful(userEnrolmentDetails))

      val result = await(sut.update(updateFunc))

      verify(mockUserDataRepository).getData(repositoryKey)(UserEnrolmentDetails.format, authenticatedRequest)
      verify(updateFunc).apply(userEnrolmentDetails)
      verify(mockUserDataRepository).putData(repositoryKey, userEnrolmentDetails)(UserEnrolmentDetails.format, authenticatedRequest)
      result mustBe userEnrolmentDetails
    }

    "delete" in {
      when(mockUserDataRepository.deleteData[UserEnrolmentDetails](any())(any(), any())).thenReturn(Future.unit)
      await(sut.delete()(authenticatedRequest))

      verify(mockUserDataRepository).deleteData(repositoryKey)(UserEnrolmentDetails.format, authenticatedRequest)
    }
  }
}
