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
import org.mockito.Mockito.{reset, verify, verifyNoInteractions, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  ETMPSubscriptionStatus,
  SubscriptionStatus
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RegistrationControllerSpec extends ControllerSpec {

  private val mcc              = stubMessagesControllerComponents()
  private val registrationPage = mock[registration_page]

  private val controller =
    new RegistrationController(authenticate = mockAuthAction,
                               mockJourneyAction,
                               mcc = mcc,
                               registration_page = registrationPage,
                               subscriptionsConnector = mockSubscriptionsConnector
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(registrationPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(registrationPage, mockRegistrationConnector, mockSubscriptionsConnector)
    super.afterEach()
  }

  "Registration Controller" should {

    "return 200" when {
      "display page method is invoked" when {

        "a 'SafeNumber' exists" in {
          authorizedUser()
          mockRegistrationFind(
            aRegistration(
              withOrganisationDetails(organisationDetails =
                registeredUkCompanyOrgDetails()
              )
            )
          )
          mockGetSubscriptionStatus(
            SubscriptionStatus(subscriptionStatus = ETMPSubscriptionStatus.NO_FORM_BUNDLE_FOUND,
                               idType = "ZPPT",
                               idValue = "XXPPTP123456789"
            )
          )
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK

          verify(mockSubscriptionsConnector).getSubscriptionStatus(any())(any())
        }

        "a 'SafeNumber' does not exist" in {
          authorizedUser()
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          verifyNoInteractions(mockSubscriptionsConnector)
        }
      }
    }
  }

  "Registration Controller" should {

    "return error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        mockUkCompanyCreateIncorpJourneyId("http://test/redirect/uk-company")
        mockGetSubscriptionStatusFailure(new RuntimeException("error"))
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }
  }
}
