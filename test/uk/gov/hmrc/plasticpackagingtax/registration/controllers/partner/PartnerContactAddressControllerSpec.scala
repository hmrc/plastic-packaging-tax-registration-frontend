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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{redirectLocation, status}
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.{
  AddressLookupAddress,
  AddressLookupConfirmation,
  AddressLookupCountry,
  AddressLookupOnRamp
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class PartnerContactAddressControllerSpec
    extends ControllerSpec with DefaultAwaitTimeout with PptTestData {

  private val mockAddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]
  private val mcc                                = stubMessagesControllerComponents()

  private val controller =
    new PartnerContactAddressController(authenticate = mockAuthAction,
                                        journeyAction = mockJourneyAction,
                                        registrationConnector = mockRegistrationConnector,
                                        addressLookupFrontendConnector =
                                          mockAddressLookupFrontendConnector,
                                        appConfig = appConfig,
                                        mcc = mcc
    )

  private val partnershipRegistrationWithInflightPartner = aRegistration(
    withPartnershipDetails(
      Some(
        generalPartnershipDetailsWithPartners.copy(
          partners = Seq(),
          inflightPartner = Some(
            aSoleTraderPartner().copy(contactDetails =
              aSoleTraderPartner().contactDetails.map(_.copy(address = None))
            )
          )
        )
      )
    )
  )

  private val partnershipRegistrationWithExistingPartner = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
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
    authorizedUser()
    mockRegistrationUpdate()
    when(mockAddressLookupFrontendConnector.initialiseJourney(any())(any(), any())).thenReturn(
      Future(AddressLookupOnRamp("/on-ramp"))
    )
    when(mockAddressLookupFrontendConnector.getAddress(any())(any(), any())).thenReturn(
      Future.successful(alfAddress)
    )
  }

  override protected def afterEach(): Unit =
    super.afterEach()

  "Partner Contact Address Controller" should {
    "Redirect to the ALF address lookup page" when {
      "capturing address for new partner" in {
        mockRegistrationFind(partnershipRegistrationWithInflightPartner)

        val resp = controller.captureNewPartner()(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some("/on-ramp")
      }
      "capturing address for existing partner" in {
        mockRegistrationFind(partnershipRegistrationWithExistingPartner)

        val resp = controller.captureExistingPartner(
          partnershipRegistrationWithExistingPartner.nominatedPartner.get.id
        )(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some("/on-ramp")
      }
    }

    "Populate returned address, promote inflight and redirect to partner check answers" when {
      "receive ALF callback for new partner" in {
        mockRegistrationFind(partnershipRegistrationWithInflightPartner)

        val resp = controller.alfCallbackNewPartner(Some("123"))(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          routes.PartnerCheckAnswersController.displayNewPartner().url
        )

        modifiedRegistration.newPartner.get.contactDetails.get.address mustBe Some(
          Address(alfAddress)
        )
      }
      "receive ALF callback for existing partner" in {
        val nominatedPartnerId = partnershipRegistrationWithExistingPartner.nominatedPartner.get.id
        mockRegistrationFind(partnershipRegistrationWithExistingPartner)

        val resp =
          controller.alfCallbackExistingPartner(nominatedPartnerId, Some("123"))(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          routes.PartnerCheckAnswersController.displayExistingPartner(nominatedPartnerId).url
        )

        modifiedRegistration.nominatedPartner.get.contactDetails.get.address mustBe Some(
          Address(alfAddress)
        )
      }
    }
  }
}
