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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.error_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class NotableErrorControllerSpec extends ControllerSpec {

  private val page = mock[error_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new NotableErrorController(mcc = mcc, errorPage = page)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply()(any(), any())).thenReturn(HtmlFormat.raw("error page content"))
  }

  "NotableErrorController" should {
    "present the generic error page on subscription failure" in {
      val resp = controller.subscriptionFailure()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error page content"
    }
  }
}
