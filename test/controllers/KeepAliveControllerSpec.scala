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

package controllers

import base.unit.ControllerSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class KeepAliveControllerSpec extends ControllerSpec {
  private val mcc = stubMessagesControllerComponents()

  private val controller =
    new KeepAliveController(journeyAction = spyJourneyAction, mcc = mcc)

  override protected def afterEach(): Unit =
    super.afterEach()

  "Keepalive controller" should {
    "keepalive" in {
      spyJourneyAction.setReg(aRegistration())
      status(controller.keepAlive()(FakeRequest())) mustBe OK
    }
  }

}
