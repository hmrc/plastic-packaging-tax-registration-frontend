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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment

import base.unit.{AddressCaptureSpec, ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.services.AddressCaptureConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import scala.concurrent.Future

class AmendOrganisationDetailsControllerSpec
    extends ControllerSpec with AddressCaptureSpec with MockAmendmentJourneyAction {

  private val mcc = stubMessagesControllerComponents()

  private val registration = aRegistration()

  private val controller =
    new AmendOrganisationDetailsController(mockAuthAllowEnrolmentAction,
                                           mcc,
                                           mockAddressCaptureService,
                                           mockAmendmentJourneyAction
    )

  override protected def beforeEach(): Unit = {
    reset(mockSubscriptionConnector)
    authorisedUserWithPptSubscription()
    inMemoryRegistrationAmendmentRepository.reset()
    simulateGetSubscriptionSuccess(registration)
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
                               pptHeadingKey = "addressLookup.contact.lookup.heading",
                               entityName = registration.organisationDetails.businessName,
                               pptHintKey = None
          )
        simulateSuccessfulAddressCaptureInit(Some(expectedAddressCaptureConfig))

        val resp: Future[Result] = controller.changeBusinessAddress()(getRequest())

        // TODO: check init config

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

    "obtain address from address lookup and update registration and redirect to amend registration page" when {
      "control is returned from address capture" in {
        authorisedUserWithPptSubscription()
        inMemoryRegistrationAmendmentRepository.put("123", registration)
        simulateValidAddressCapture()

        val resp = controller.addressCaptureCallback()(getRequest())

        redirectLocation(resp) mustBe Some(routes.AmendRegistrationController.displayPage().url)
        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockSubscriptionConnector).updateSubscription(any(), registrationCaptor.capture())(
          any()
        )

        registrationCaptor.getValue.organisationDetails.businessRegisteredAddress mustBe Some(
          validCapturedAddress
        )
      }
    }

  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
