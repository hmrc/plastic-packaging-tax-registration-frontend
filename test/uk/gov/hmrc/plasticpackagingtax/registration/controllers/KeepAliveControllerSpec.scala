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
import base.unit.{ControllerSpec, MockRegistrationAmendmentRepository}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AuthenticatedRequest,
  KeepAliveActionImpl
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext

class KeepAliveControllerSpec extends ControllerSpec with MockRegistrationAmendmentRepository {
  private val mcc = stubMessagesControllerComponents()

  private val keepAliveAction =
    new KeepAliveActionImpl(appConfig, inMemoryRegistrationAmendmentRepository)(
      ExecutionContext.global
    )

  private val controller =
    new KeepAliveController(authenticate = mockAuthAllowEnrolmentAction,
                            keepAliveAction = keepAliveAction,
                            mcc = mcc
    )

  override protected def beforeEach(): Unit =
    inMemoryRegistrationAmendmentRepository.reset()

  super.beforeEach()

  override protected def afterEach(): Unit =
    super.afterEach()

  "Keepalive controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        val cachedRegistration = aRegistration().copy(id = "3453456")
        inMemoryRegistrationAmendmentRepository.put("123", cachedRegistration)
        authorizedUser()
        val result = controller.keepAlive()(
          new AuthenticatedRequest(FakeRequest().withSession(("sessionId", "123")), newUser())
        )

        status(result) mustBe OK
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
