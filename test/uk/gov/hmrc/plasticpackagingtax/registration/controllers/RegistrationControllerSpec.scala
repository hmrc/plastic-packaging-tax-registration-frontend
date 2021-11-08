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
import org.mockito.Mockito.{reset, verifyNoInteractions, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.forms.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  registration_group,
  registration_single_entity
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RegistrationControllerSpec extends ControllerSpec {

  private val mcc              = stubMessagesControllerComponents()
  private val singleEntityPage = mock[registration_single_entity]
  private val groupPage        = mock[registration_group]

  private val controller =
    new RegistrationController(authenticate = mockAuthAction,
                               mockJourneyAction,
                               mcc = mcc,
                               singleEntityPage = singleEntityPage,
                               groupPage = groupPage
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(singleEntityPage.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("Single Entity Page")
    )
    when(groupPage.apply(any())(any(), any())).thenReturn(HtmlFormat.raw("Group Page"))
  }

  override protected def afterEach(): Unit = {
    reset(singleEntityPage, mockRegistrationConnector, mockSubscriptionsConnector)
    super.afterEach()
  }

  "Registration Controller" should {

    "return 200" when {
      "display registration page" when {

        "a 'businessPartnerId' exists" in {
          authorizedUser()
          mockRegistrationFind(
            aRegistration(
              withOrganisationDetails(organisationDetails =
                registeredUkCompanyOrgDetails()
              )
            )
          )

          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Single Entity Page"
        }

        "a 'businessPartnerId' does not exist" in {
          authorizedUser()
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Single Entity Page"

          verifyNoInteractions(mockSubscriptionsConnector)
        }

        "a group registration is being performed" in {
          authorizedUser()
          mockRegistrationFind(aRegistration().copy(registrationType = Some(RegType.GROUP)))
          val result = controller.displayPage()(getRequest())

          status(result) mustBe OK
          contentAsString(result) mustBe "Group Page"
        }
      }
    }

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
