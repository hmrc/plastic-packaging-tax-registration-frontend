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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => commonRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup._
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class ConfirmBusinessAddressControllerSpec extends ControllerSpec {
  private val page                               = mock[confirm_business_address]
  private val mcc                                = stubMessagesControllerComponents()
  private val mockAddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]

  private val controller =
    new ConfirmBusinessAddressController(authenticate = mockAuthAction,
                                         journeyAction = mockJourneyAction,
                                         registrationConnector = mockRegistrationConnector,
                                         addressLookupFrontendConnector =
                                           mockAddressLookupFrontendConnector,
                                         appConfig,
                                         mcc = mcc,
                                         page = page
    )

  private val alfAddress = AddressLookupConfirmation(auditRef = "auditRef",
                                                     id = Some("123"),
                                                     address = AddressLookupAddress(
                                                       lines = List("addressLine1",
                                                                    "addressLine2",
                                                                    "addressLine3"
                                                       ),
                                                       postcode = Some("E17 1ER"),
                                                       country = Some(
                                                         AddressLookupCountry(code = "GB",
                                                                              name =
                                                                                "United Kingdom"
                                                         )
                                                       )
                                                     )
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Address], any(), any())(any(), any())).thenReturn(
      HtmlFormat.raw("business registered address")
    )
    authorizedUser()
    mockRegistrationUpdate()
    when(mockAddressLookupFrontendConnector.initialiseJourney(any())(any(), any())).thenReturn(
      Future(AddressLookupOnRamp("/on-ramp"))
    )
    when(mockAddressLookupFrontendConnector.getAddress(any())(any(), any())).thenReturn(
      Future.successful(alfAddress)
    )
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Confirm Company Address Controller" should {

    "display registered business address when it is populated and valid" in {
      mockRegistrationFind(aRegistration())

      val resp = controller.displayPage()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "business registered address"
    }

    "redirect to address lookup frontend" when {
      "registered business address is not present" in {
        val registration = aRegistration()
        mockRegistrationFind(
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(businessRegisteredAddress = None)
          )
        )

        val resp = controller.displayPage()(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some("/on-ramp")
      }
      "registered business address is invalid" in {
        val registration = aRegistration()
        mockRegistrationFind(
          registration.copy(organisationDetails =
            registration.organisationDetails.copy(businessRegisteredAddress =
              registration.organisationDetails.businessRegisteredAddress.map(
                _.copy(addressLine1 =
                  "100 Really Long Street Name Which is Well in Excess of 35 characters"
                )
              )
            )
          )
        )

        val resp = controller.displayPage()(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some("/on-ramp")
      }
    }

    "obtain address from address lookup and update registration and redirect to task list" when {
      "control is returned from address lookup frontend" in {
        val resp = controller.alfCallback(Some("123"))(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(commonRoutes.TaskListController.displayPage().url)

        modifiedRegistration.organisationDetails.businessRegisteredAddress mustBe Some(
          Address(alfAddress)
        )
      }
    }

    "throw MissingAddressIdException if return from address lookup is missing a journey id" in {
      intercept[MissingAddressIdException] {
        await(controller.alfCallback(None)(getRequest()))
      }
    }
  }
}
