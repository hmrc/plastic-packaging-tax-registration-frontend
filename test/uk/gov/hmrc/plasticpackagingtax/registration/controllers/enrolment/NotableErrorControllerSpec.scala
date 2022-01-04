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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.{
  reference_number_already_used_failure_page,
  verification_failure_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class NotableErrorControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val enrolmentVerificationFailurePage = mock[verification_failure_page]

  private val enrolmentReferenceNumberAlreadyUsedFailurePage =
    mock[reference_number_already_used_failure_page]

  private val controller =
    new NotableErrorController(authenticate = mockAuthAction,
                               mcc = mcc,
                               verificationFailurePage = enrolmentVerificationFailurePage,
                               referenceNumberAlreadyUsedPage =
                                 enrolmentReferenceNumberAlreadyUsedFailurePage
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(enrolmentVerificationFailurePage.apply()(any(), any())).thenReturn(
      HtmlFormat.raw("error business not verified content")
    )
    when(enrolmentReferenceNumberAlreadyUsedFailurePage.apply()(any(), any())).thenReturn(
      HtmlFormat.raw("error ppt reference already been used content")
    )
  }

  "NotableErrorController" should {

    "present the business not verified failed page" in {
      authorizedUser()
      val resp = controller.enrolmentVerificationFailurePage()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error business not verified content"
    }

    "present the ppt reference number already been used failure page" in {
      authorizedUser()
      val resp = controller.enrolmentReferenceNumberAlreadyUsedPage()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error ppt reference already been used content"
    }
  }
}
