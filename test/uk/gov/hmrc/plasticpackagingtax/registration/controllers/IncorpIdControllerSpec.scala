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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.unit.ControllerSpec
import controllers.Assets.SEE_OTHER
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.verify
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class IncorpIdControllerSpec extends ControllerSpec {

  private val fakeRequest = FakeRequest("GET", "/")
  private val mcc         = stubMessagesControllerComponents()

  private val controller =
    new IncorpIdController(authenticate = mockAuthAction,
                           mockJourneyAction,
                           mockRegistrationConnector,
                           mcc
    )(config, ec)

  "incorpIdCallback" should {

    "redirect to the registration page" in {
      authorizedUser()
      given(mockRegistrationConnector.update(any())(any())).willReturn(
        Future(Right(Registration("uuid-id")))
      )
      val result = controller.incorpIdCallback("uuid-id")(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
    }

    "update registration with journey id" in {
      authorizedUser()
      given(mockRegistrationConnector.update(any())(any())).willReturn(
        Future(Right(Registration("uuid-id")))
      )
      await(controller.incorpIdCallback("uuid-id")(fakeRequest))

      getRegistration.incorpJourneyId mustBe Some("uuid-id")
    }

    "throw exception when journey id update fails" in {
      authorizedUser()
      given(mockRegistrationConnector.update(any())(any())).willReturn(
        Future(Left(DownstreamServiceError("error", new RuntimeException())))
      )

      intercept[DownstreamServiceError] {
        await(controller.incorpIdCallback("uuid-id")(fakeRequest))
      }
    }
  }

  def getRegistration: Registration = {
    val captor = ArgumentCaptor.forClass(classOf[Registration])
    verify(mockRegistrationConnector, Mockito.atLeastOnce()).update(captor.capture())(any())
    captor.getValue
  }

}
