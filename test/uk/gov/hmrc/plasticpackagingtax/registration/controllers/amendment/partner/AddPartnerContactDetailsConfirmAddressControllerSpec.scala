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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner

import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.{
  AddressLookupAddress,
  AddressLookupConfirmation,
  AddressLookupCountry,
  AddressLookupOnRamp
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.AmendRegistrationUpdateService
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.RegistrationAmendmentRepository
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import scala.concurrent.Future

class AddPartnerContactDetailsConfirmAddressControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction with PptTestData {

  private val mockAddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]
  private val mcc                                = stubMessagesControllerComponents()

  private val mockRegistrationAmendmentRepository = mock[RegistrationAmendmentRepository]

  protected val mockAmendRegistrationUpdater = new AmendRegistrationUpdateService(
    mockRegistrationAmendmentRepository
  )

  private val controller =
    new AddPartnerContactDetailsConfirmAddressController(
      authenticate = mockAuthAllowEnrolmentAction,
      journeyAction = mockAmendmentJourneyAction,
      registrationUpdater = mockAmendRegistrationUpdater,
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
    inMemoryRegistrationAmendmentRepository.reset()
    reset(mockSubscriptionConnector)
    simulateGetSubscriptionSuccess(partnershipRegistrationWithInflightPartner)
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
        authorisedUserWithPptSubscription()

        val resp = controller.displayPage()(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some("/on-ramp")
      }
    }

    "Populate returned address redirect to partner check answers" when {
      "receive ALF callback for new partner" in {
        authorisedUserWithPptSubscription()
        when(
          mockAmendRegistrationUpdater.updateRegistration(
            _ => partnershipRegistrationWithInflightPartner
          )(any(), any())
        ).thenReturn(Future.successful(partnershipRegistrationWithInflightPartner))
        val resp = controller.addressCaptureCallback(Some("123"))(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          routes.AddPartnerContactDetailsCheckAnswersController.displayPage().url
        )
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
