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

package controllers.partner

import base.unit.{AddressCaptureSpec, ControllerSpec}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.redirectLocation
import spec.PptTestData
import models.registration.NewRegistrationUpdateService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerContactAddressControllerSpec
    extends ControllerSpec with AddressCaptureSpec with DefaultAwaitTimeout with PptTestData {

  private val mcc = stubMessagesControllerComponents()

  protected val newRegistrationUpdater = new NewRegistrationUpdateService(mockRegistrationConnector)

  private val controller =
    new PartnerContactAddressController(journeyAction = spyJourneyAction,
                                        registrationUpdater = newRegistrationUpdater,
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

  private val partnershipRegistrationWithExistingPartner = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    mockRegistrationUpdate()
    simulateSuccessfulAddressCaptureInit(None)
    simulateValidAddressCapture()
  }

  override protected def afterEach(): Unit =
    super.afterEach()

  "Partner Contact Address Controller" should {
    "Redirect to address capture" when {
      "capturing address for new partner" in {
        spyJourneyAction.setReg(partnershipRegistrationWithInflightPartner)

        val resp = controller.captureNewPartner()(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
      "capturing address for existing partner" in {
        spyJourneyAction.setReg(partnershipRegistrationWithExistingPartner)

        val resp = controller.captureExistingPartner(
          partnershipRegistrationWithExistingPartner.nominatedPartner.get.id
        )(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

    "Populate returned address, promote inflight and redirect to partner check answers" when {
      "receive address capture callback for new partner" in {
        spyJourneyAction.setReg(partnershipRegistrationWithInflightPartner)

        val resp = controller.addressCaptureCallbackNewPartner()(FakeRequest())

        redirectLocation(resp) mustBe Some(
          routes.PartnerCheckAnswersController.displayNewPartner().url
        )

        modifiedRegistration.newPartner.get.contactDetails.get.address mustBe Some(
          validCapturedAddress
        )
      }
      "receive address capture callback for existing partner" in {
        val nominatedPartnerId = partnershipRegistrationWithExistingPartner.nominatedPartner.get.id
        spyJourneyAction.setReg(partnershipRegistrationWithExistingPartner)

        val resp =
          controller.addressCaptureCallbackExistingPartner(nominatedPartnerId)(FakeRequest())

        redirectLocation(resp) mustBe Some(
          routes.PartnerCheckAnswersController.displayExistingPartner(nominatedPartnerId).url
        )

        modifiedRegistration.nominatedPartner.get.contactDetails.get.address mustBe Some(
          validCapturedAddress
        )
      }
    }
  }
}
