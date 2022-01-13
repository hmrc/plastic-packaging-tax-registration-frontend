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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{await, redirectLocation, status}
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class ContactDetailsConfirmAddressControllerSpec extends ControllerSpec {
  private val mcc                                = stubMessagesControllerComponents()
  private val mockAddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]

  private val controller =
    new ContactDetailsConfirmAddressController(authenticate = mockAuthAction,
                                               journeyAction = mockJourneyAction,
                                               registrationConnector = mockRegistrationConnector,
                                               addressLookupFrontendConnector =
                                                 mockAddressLookupFrontendConnector,
                                               appConfig,
                                               mcc = mcc
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

  "Confirm Member Contact Address Controller" should {

    "redirect to address lookup frontend" when {
      "registered business address is not present" in {

        val resp = controller.displayPage(groupMember.id)(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some("/on-ramp")
      }
    }

    "obtain address from address lookup and update registration and redirect to organisation list" when {
      "control is returned from address lookup frontend" in {
        mockRegistrationFind(
          aRegistration(
            withGroupDetail(groupDetail = Some(groupDetails.copy(members = Seq(groupMember))))
          )
        )
        val resp = controller.alfCallback(Some("123"), groupMember.id)(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          groupRoutes.ContactDetailsCheckAnswersController.displayPage(groupMember.id).url
        )

        modifiedRegistration.lastMember.get.contactDetails.get.address mustBe Some(
          Address(alfAddress)
        )
      }
    }

    "throw MissingAddressIdException if return from address lookup is missing a journey id" in {
      intercept[MissingAddressIdException] {
        await(controller.alfCallback(None, groupMember.id)(getRequest()))
      }
    }
  }
}
