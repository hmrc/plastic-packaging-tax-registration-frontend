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

package base.unit

import builders.RegistrationBuilder
import connectors.{DownstreamServiceError, RegistrationConnector, ServiceError}
import models.registration.Registration
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{verify, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.ScalaOngoingStubbing
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

trait MockRegistrationConnector
    extends MockitoSugar with RegistrationBuilder with BeforeAndAfterEach {
  self: Suite =>

  protected val mockRegistrationConnector = mock[RegistrationConnector]

  def mockRegistrationUpdate(): ScalaOngoingStubbing[Future[Either[ServiceError, Registration]]] =
    when(mockRegistrationConnector.update(any[Registration])(any()))
      .thenAnswer((inv: InvocationOnMock) => Future.successful(Right(inv.getArgument(0))))

  def mockRegistrationException(): ScalaOngoingStubbing[Future[Either[ServiceError, Registration]]] =
    when(mockRegistrationConnector.update(any[Registration])(any()))
      .thenThrow(new RuntimeException("some error"))

  def mockRegistrationUpdateFailure(): ScalaOngoingStubbing[Future[Either[ServiceError, Registration]]] =
    when(mockRegistrationConnector.update(any[Registration])(any()))
      .thenReturn(
        Future.successful(Left(DownstreamServiceError("some error", new Exception("some error"))))
      )

  def modifiedRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector).update(captor.capture())(any())
    captor.getValue
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    Mockito.reset(mockRegistrationConnector)
  }

}
