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

package controllers.amendment.partner

import base.unit.{AddressCaptureSpec, ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.Mockito.reset
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation}
import spec.PptTestData
import models.registration.AmendRegistrationUpdateService
import models.request.AmendmentJourneyAction
import services.AddressCaptureConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport._

import scala.concurrent.Future

class AddPartnerContactDetailsConfirmAddressControllerSpec
    extends ControllerSpec with AddressCaptureSpec with MockAmendmentJourneyAction
    with PptTestData {

  private val mcc       = stubMessagesControllerComponents()
  private val sessionId = "123"

  private val controller =
    new AddPartnerContactDetailsConfirmAddressController(
      authenticate = mockEnrolledAuthAction,
      journeyAction = mockAmendmentJourneyAction,
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
            aSoleTraderPartner().copy(contactDetails =
              aSoleTraderPartner().contactDetails.map(_.copy(address = None))
            )
          )
        )
      )
    )
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    inMemoryRegistrationAmendmentRepository.reset()
    reset(mockSubscriptionConnector)
    simulateGetSubscriptionSuccess(partnershipRegistrationWithInflightPartner)
  }

  override protected def afterEach(): Unit =
    super.afterEach()

  "Partner Contact Address Controller" should {
    "redirect to address capture" when {
      "capturing address for new partner" in {
        authorisedUserWithPptSubscription()

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

        val resp = controller.displayPage()(getRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

    "update registration with captured address and redirect to partner check answers" when {
      "address capture callback called" in {
        authorisedUserWithPptSubscription()
        simulateValidAddressCapture()
        simulateUpdateSubscriptionSuccess()

        val resp = await(controller.addressCaptureCallback()(getRequest()))

        inMemoryRegistrationAmendmentRepository.get(sessionId).map { registration =>
          registration.get.inflightPartner.get.contactDetails.get.address mustBe validCapturedAddress
        }

        redirectLocation(Future.successful(resp)) mustBe Some(
          routes.AddPartnerContactDetailsCheckAnswersController.displayPage().url
        )
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, sessionId)).withCSRFToken

}
