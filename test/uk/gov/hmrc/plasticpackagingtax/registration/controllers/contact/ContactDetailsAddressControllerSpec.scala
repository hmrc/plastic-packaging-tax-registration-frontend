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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

import base.unit.{AddressCaptureSpec, ControllerSpec}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.plasticpackagingtax.registration.services.AddressCaptureConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsAddressControllerSpec extends ControllerSpec with AddressCaptureSpec {

  private val mcc = stubMessagesControllerComponents()

  private val registration = aRegistration()

  private val controller =
    new ContactDetailsAddressController(authenticate = mockAuthAction,
                                        mockJourneyAction,
                                        mockRegistrationConnector,
                                        mockAddressCaptureService,
                                        mcc = mcc
    )

  "Primary Contact Details Address Controller" should {

    "redirect to address capture" when {

      "display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(registration)
        val expectedAddressCaptureConfig =
          AddressCaptureConfig(backLink =
                                 routes.ContactDetailsConfirmAddressController.displayPage().url,
                               successLink = routes.ContactDetailsAddressController.update.url,
                               alfHeadingsPrefix = "addressLookup.contact",
                               pptHeadingKey = "addressLookup.contact.lookup.heading",
                               entityName = registration.primaryContactDetails.name,
                               pptHintKey = None
          )
        simulateSuccessfulAddressCaptureInit(Some(expectedAddressCaptureConfig))

        val result = controller.displayPage()(getRequest())

        redirectLocation(result) mustBe Some(addressCaptureRedirect.url)
      }
    }

    "update" should {
      "persist and redirect when compatible address is returned" in {
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
        simulateValidAddressCapture()

        val result = controller.update()(getRequest())

        redirectLocation(result) mustBe Some(
          routes.ContactDetailsCheckAnswersController.displayPage().url
        )

        modifiedRegistration.primaryContactDetails.address mustBe Some(validCapturedAddress)
      }
    }

  }
}
