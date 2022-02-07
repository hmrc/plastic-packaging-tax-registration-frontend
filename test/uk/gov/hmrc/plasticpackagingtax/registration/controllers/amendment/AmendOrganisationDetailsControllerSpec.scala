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

import base.PptTestData.newUser
import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup._
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

import scala.concurrent.Future

class AmendOrganisationDetailsControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction {
  private val mcc                                = stubMessagesControllerComponents()
  private val mockAddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]

  private val controller =
    new AmendOrganisationDetailsController(mockAuthAllowEnrolmentAction,
                                           mcc,
                                           mockAddressLookupFrontendConnector,
                                           appConfig,
                                           mockAmendmentJourneyAction
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
    reset(mockSubscriptionConnector)
    authorisedUserWithPptSubscription()
    inMemoryRegistrationAmendmentRepository.reset()
    simulateGetSubscriptionSuccess(aRegistration())
    when(mockAddressLookupFrontendConnector.initialiseJourney(any())(any(), any())).thenReturn(
      Future(AddressLookupOnRamp("/on-ramp"))
    )
    when(mockAddressLookupFrontendConnector.getAddress(any())(any(), any())).thenReturn(
      Future.successful(alfAddress)
    )
  }

  "Amend Organisation Details Controller" should {

    "redirect to address lookup frontend" when {
      "change business address" in {

        val resp: Future[Result] = controller.changeBusinessAddress()(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some("/on-ramp")
      }
    }

    "obtain address from address lookup and update registration and redirect to amend registration page" when {
      "control is returned from address lookup frontend" in {
        val registration = aRegistration()
        authorisedUserWithPptSubscription()
        inMemoryRegistrationAmendmentRepository.put("123", registration)
        val resp = controller.alfCallback(Some("123"))(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.AmendRegistrationController.displayPage().url)
        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockSubscriptionConnector).updateSubscription(any(), registrationCaptor.capture())(
          any()
        )

        registrationCaptor.getValue.organisationDetails.businessRegisteredAddress mustBe Some(
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

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
