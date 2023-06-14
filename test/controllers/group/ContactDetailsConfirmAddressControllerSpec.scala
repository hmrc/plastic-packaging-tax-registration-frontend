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

package controllers.group

import base.unit.{AddressCaptureSpec, ControllerSpec}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers.{await, redirectLocation}
import controllers.group.{routes => groupRoutes}
import models.registration.{GroupDetail, NewRegistrationUpdateService}
import play.api.test.FakeRequest
import services.AddressCaptureConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsConfirmAddressControllerSpec extends ControllerSpec with AddressCaptureSpec {

  private val mcc = stubMessagesControllerComponents()

  private val groupRegistration = aRegistration(
    withGroupDetail(
      Some(GroupDetail(membersUnderGroupControl = Some(true), members = Seq(groupMember)))
    )
  )

  private val mockNewRegistrationUpdater = new NewRegistrationUpdateService(
    mockRegistrationConnector
  )

  private val controller =
    new ContactDetailsConfirmAddressController(journeyAction = spyJourneyAction,
                                               addressCaptureService = mockAddressCaptureService,
                                               mcc = mcc,
                                               mockNewRegistrationUpdater
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    spyJourneyAction.setReg(groupRegistration)
    mockRegistrationUpdate()
  }

  override protected def afterEach(): Unit =
    super.afterEach()

  "Confirm Member Contact Address Controller" should {

    "redirect to address lookup frontend" when {
      "registered business address is not present" in {
        val expectedAddressCaptureConfig =
          AddressCaptureConfig(
            backLink =
              routes.ContactDetailsTelephoneNumberController.displayPage(groupMember.id).url,
            successLink =
              routes.ContactDetailsConfirmAddressController.addressCaptureCallback(
                groupMember.id
              ).url,
            alfHeadingsPrefix = "addressLookup.partner",
            pptHeadingKey = "addressCapture.contact.heading",
            entityName = Some(groupMember.businessName),
            pptHintKey = None,
            forceUkAddress = false
          )
        simulateSuccessfulAddressCaptureInit(Some(expectedAddressCaptureConfig))

        val resp = controller.displayPage(groupMember.id)(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

    "obtain address from address capture and update registration and redirect to organisation list" when {
      "control is returned from address capture" in {
        spyJourneyAction.setReg(
          aRegistration(
            withGroupDetail(groupDetail = Some(groupDetails.copy(members = Seq(groupMember))))
          )
        )
        simulateValidAddressCapture()

        val resp = controller.addressCaptureCallback(groupMember.id)(FakeRequest())

        redirectLocation(resp) mustBe Some(
          groupRoutes.ContactDetailsCheckAnswersController.displayPage(groupMember.id).url
        )

        modifiedRegistration.lastMember.get.contactDetails.get.address mustBe Some(
          validCapturedAddress
        )
      }
    }

    "throw IllegalStateException" when {
      "group member cannot be found in registration" in {
        intercept[IllegalStateException] {
          await(controller.addressCaptureCallback("XXX")(FakeRequest()))
        }
      }
    }
  }
}
