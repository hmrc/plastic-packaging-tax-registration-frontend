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
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.{
  SubscriptionStatus,
  SubscriptionStatusResponse
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  duplicate_subscription_page,
  registration_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RegistrationControllerSpec extends ControllerSpec {

  private val mcc                       = stubMessagesControllerComponents()
  private val registrationPage          = mock[registration_page]
  private val duplicateSubscriptionPage = mock[duplicate_subscription_page]

  private val controller =
    new RegistrationController(authenticate = mockAuthAction,
                               mockJourneyAction,
                               mcc = mcc,
                               registrationPage = registrationPage,
                               duplicateSubscriptionPage = duplicateSubscriptionPage,
                               subscriptionsConnector = mockSubscriptionsConnector
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(registrationPage.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("Registration Page")
    )
    when(duplicateSubscriptionPage.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("Duplicate Page")
    )
  }

  override protected def afterEach(): Unit = {
    reset(registrationPage,
          duplicateSubscriptionPage,
          mockRegistrationConnector,
          mockSubscriptionsConnector
    )
    super.afterEach()
  }

  "Registration Controller" should {

    "return 200" when {
      "display registration page" when {

        "a 'businessPartnerId' exists that is not already subscribed" in {
          authorizedUser()
          mockRegistrationFind(
            aRegistration(
              withOrganisationDetails(organisationDetails =
                registeredUkCompanyOrgDetails()
              )
            )
          )
          mockGetSubscriptionStatus(
            SubscriptionStatusResponse(status = SubscriptionStatus.NOT_SUBSCRIBED)
          )
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Registration Page"

          verify(mockSubscriptionsConnector).getSubscriptionStatus(any())(any())
        }

        "a 'businessPartnerId' does not exist" in {
          authorizedUser()
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Registration Page"

          verifyNoInteractions(mockSubscriptionsConnector)
        }
      }

      "display duplicate subscription page" when {

        "a 'businessPartnerId' exists that is already subscribed" in {
          authorizedUser()
          mockRegistrationFind(
            aRegistration(
              withOrganisationDetails(organisationDetails =
                registeredUkCompanyOrgDetails()
              )
            )
          )
          mockGetSubscriptionStatus(
            SubscriptionStatusResponse(status = SubscriptionStatus.SUBSCRIBED,
                                       pptReference = Some("XXPPTP123456789")
            )
          )
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Duplicate Page"

          verify(mockSubscriptionsConnector).getSubscriptionStatus(any())(any())
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
