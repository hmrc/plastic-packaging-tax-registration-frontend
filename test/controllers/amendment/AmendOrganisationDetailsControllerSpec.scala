/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.amendment

import base.unit.{AddressCaptureSpec, AmendmentControllerSpec, ControllerSpec}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import services.AddressCaptureConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport._

import scala.concurrent.Future

class AmendOrganisationDetailsControllerSpec
    extends ControllerSpec with AddressCaptureSpec with AmendmentControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val registration = aRegistration()

  private val controller =
    new AmendOrganisationDetailsController(spyJourneyAction,
      mockAmendRegService,
      mcc,
      mockAddressCaptureService
    )

  override protected def beforeEach(): Unit = {

    inMemoryRegistrationAmendmentRepository.reset()
    spyJourneyAction.setReg(registration)
    simulateUpdateSubscriptionSuccess()
  }

  "Amend Organisation Details Controller" should {

    "redirect to address lookup frontend" when {
      "change business address" in {
        val expectedAddressCaptureConfig =
          AddressCaptureConfig(backLink = routes.AmendRegistrationController.displayPage().url,
                               successLink =
                                 routes.AmendOrganisationDetailsController.addressCaptureCallback().url,
                               alfHeadingsPrefix = "addressLookup.business",
                               pptHeadingKey = "addressCapture.business.heading",
                               entityName = registration.organisationDetails.businessName,
                               pptHintKey = None,
                               forceUkAddress = false
          )
        simulateSuccessfulAddressCaptureInit(Some(expectedAddressCaptureConfig))

        val resp: Future[Result] = controller.changeBusinessAddress()(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

    "obtain address from address lookup and update registration and redirect to amend registration page" when {
      "control is returned from address capture" in {

        inMemoryRegistrationAmendmentRepository.put("123", registration)
        simulateValidAddressCapture()

        val resp = controller.addressCaptureCallback()(FakeRequest())

        redirectLocation(resp) mustBe Some(routes.AmendRegistrationController.displayPage().url)

        verify(mockAmendRegService).updateSubscriptionWithRegistration(any())(any(), any())
        val updatedRegistration = getUpdatedRegistrationMethod().apply(registration)
        updatedRegistration.organisationDetails.businessRegisteredAddress mustBe Some(
          validCapturedAddress
        )
      }
    }

  }

}
