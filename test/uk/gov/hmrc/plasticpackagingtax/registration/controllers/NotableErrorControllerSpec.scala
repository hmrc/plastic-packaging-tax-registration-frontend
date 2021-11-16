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
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.grs_failure_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.business_verification_failure_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  duplicate_subscription_page,
  enrolment_failure_page,
  error_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class NotableErrorControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val errorPage                      = mock[error_page]
  private val enrolmentFailurePage           = mock[enrolment_failure_page]
  private val grsFailurePage                 = mock[grs_failure_page]
  private val businessVerificationFailedPage = mock[business_verification_failure_page]
  private val duplicateSubscriptionPage      = mock[duplicate_subscription_page]

  private val controller =
    new NotableErrorController(authenticate = mockAuthAction,
                               mockJourneyAction,
                               mcc = mcc,
                               errorPage = errorPage,
                               errorNoSavePage = enrolmentFailurePage,
                               grsFailurePage = grsFailurePage,
                               businessVerificationFailurePage = businessVerificationFailedPage,
                               duplicateSubscriptionPage = duplicateSubscriptionPage
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(errorPage.apply()(any(), any())).thenReturn(HtmlFormat.raw("error page content"))
    when(enrolmentFailurePage.apply()(any(), any(), any())).thenReturn(
      HtmlFormat.raw("error no save page content")
    )
    when(grsFailurePage.apply()(any(), any())).thenReturn(HtmlFormat.raw("grs failure content"))
    when(businessVerificationFailedPage.apply()(any(), any())).thenReturn(
      HtmlFormat.raw("error business verification failed content")
    )
    when(duplicateSubscriptionPage.apply()(any(), any())).thenReturn(
      HtmlFormat.raw("duplicate subscription content")
    )
  }

  "NotableErrorController" should {
    "present the generic error page on subscription failure" in {
      authorizedUser()
      val resp = controller.subscriptionFailure()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error page content"
    }

    "present the generic error no save page on enrolment failure" in {
      authorizedUser()
      val resp = controller.enrolmentFailure()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error no save page content"
    }

    "present the business verification failed page" in {
      authorizedUser()
      val resp = controller.businessVerificationFailure()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error business verification failed content"
    }

    "present the grs failure page" in {
      val resp = controller.grsFailure()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "grs failure content"
    }

    "present the duplicate subscription page" in {
      val resp = controller.duplicateRegistration()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "duplicate subscription content"
    }
  }
}
