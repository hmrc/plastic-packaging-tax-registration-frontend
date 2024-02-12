/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.amendment.partner

import base.unit.{AddressCaptureSpec, AmendmentControllerSpec, ControllerSpec, MockRegistrationAmendmentRepository}
import models.registration.AmendRegistrationUpdateService
import org.mockito.MockitoSugar.reset
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation}
import services.AddressCaptureConfig
import spec.PptTestData
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class AddPartnerContactDetailsConfirmAddressControllerSpec
    extends ControllerSpec with MockRegistrationAmendmentRepository with AddressCaptureSpec with AmendmentControllerSpec
    with PptTestData {

  private val mcc       = stubMessagesControllerComponents()
  private val sessionId = "123"

  private val controller =
    new AddPartnerContactDetailsConfirmAddressController(
      journeyAction = spyJourneyAction,
      registrationUpdater =
        new AmendRegistrationUpdateService(inMemoryRegistrationAmendmentRepository),
      addressCaptureService = mockAddressCaptureService,
      mcc = mcc
    )

  private val partnershipRegistrationWithInflightPartner = aRegistration(
    withPartnershipDetails(
      Some(
        generalPartnershipDetailsWithPartners.copy(
          partners = Seq(),
          inflightPartner = Some(
            aSoleTraderPartner.copy(contactDetails =
              aSoleTraderPartner.contactDetails.map(_.copy(address = None))
            )
          )
        )
      )
    )
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    inMemoryRegistrationAmendmentRepository.reset()
    spyJourneyAction.setReg(partnershipRegistrationWithInflightPartner)
    reset(mockSubscriptionConnector)
    simulateGetSubscriptionSuccess(partnershipRegistrationWithInflightPartner)
  }

  override protected def afterEach(): Unit =
    super.afterEach()

  "Partner Contact Address Controller" should {
    "redirect to address capture" when {
      "capturing address for new partner" in {

        val expectedAddressCaptureConfig =
          AddressCaptureConfig(
            backLink = routes.AddPartnerContactDetailsTelephoneNumberController.displayPage().url,
            successLink =
              routes.AddPartnerContactDetailsConfirmAddressController.addressCaptureCallback().url,
            alfHeadingsPrefix = "addressLookup.partner",
            pptHeadingKey = "addressCapture.contact.heading",
            entityName = partnershipRegistrationWithInflightPartner.inflightPartner.map(_.name),
            pptHintKey = None,
            forceUkAddress = false
          )
        simulateSuccessfulAddressCaptureInit(Some(expectedAddressCaptureConfig))

        val resp = controller.displayPage()(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

    "update registration with captured address and redirect to partner check answers" when {
      "address capture callback called" in {
        simulateValidAddressCapture()
        simulateUpdateSubscriptionSuccess()

        inMemoryRegistrationAmendmentRepository.put(partnershipRegistrationWithInflightPartner)

        val resp = await(controller.addressCaptureCallback()(FakeRequest()))

        inMemoryRegistrationAmendmentRepository.get(sessionId).map { registration =>
          registration.get.inflightPartner.get.contactDetails.get.address mustBe validCapturedAddress
        }

        redirectLocation(Future.successful(resp)) mustBe Some(
          routes.AddPartnerContactDetailsCheckAnswersController.displayPage().url
        )
      }
    }
  }

}
