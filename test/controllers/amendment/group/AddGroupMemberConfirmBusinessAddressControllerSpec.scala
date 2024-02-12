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

package controllers.amendment.group

import base.unit.{AddressCaptureSpec, AmendmentControllerSpec, ControllerSpec}
import forms.contact.Address
import forms.contact.Address.UKAddress
import models.registration.GroupDetail
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.organisation.confirm_business_address

class AddGroupMemberConfirmBusinessAddressControllerSpec extends ControllerSpec with AddressCaptureSpec
  with AmendmentControllerSpec with PptTestData {

  private val page = mock[confirm_business_address]
  private val mcc = stubMessagesControllerComponents()

  private val controller =
    new AddGroupMemberConfirmBusinessAddressController(
      journeyAction = spyJourneyAction,
      registrationConnector = mockRegistrationConnector,
      mockAddressCaptureService,
      mcc = mcc,
      page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    spyJourneyAction.reset()
    reset(spyJourneyAction)
    when(page.apply(any[Address], any(), any())(any(), any())).thenReturn(HtmlFormat.raw("business registered address"))
    mockRegistrationUpdate()
    simulateSuccessfulAddressCaptureInit(None)
    simulateValidAddressCapture()
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  val testMemberId = "dfd64d98-9274-496b-9801-5390da22660e"

  ".displayPage" should {

    "display business address for confirmation when it is populated and valid" in {

      val reg = aRegistration(withGroupDetail(
        Some(GroupDetail(Some(false), Seq(aGroupMember().copy(id = testMemberId)))))
      )

      spyJourneyAction.setReg(reg)

      val resp = controller.displayPage(testMemberId)(FakeRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "business registered address"
      verify(spyJourneyAction).amend
    }

    "redirect to address capture" when {

      "business address is invalid" in {

        val reg = aRegistration(withGroupDetail(
          Some(GroupDetail(Some(false), Seq(aGroupMember().copy(id = testMemberId, addressDetails =
          UKAddress(
            addressLine1 = "100 Really Long Street Name Which is Well in Excess of 35 characters",
            addressLine2 = None,
            addressLine3 = None,
            townOrCity = "town",
            postCode = "TF1 1AA"
          )
          )))))
        )

        spyJourneyAction.setReg(reg)

        val resp = controller.displayPage(testMemberId)(FakeRequest())

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
        verify(spyJourneyAction).amend
      }
    }

    ".addressCaptureCallback" when {
      "address is retrieved from address capture service, reg is updated" should {
        "redirect to group member contact name" in {

          val reg = aRegistration(withGroupDetail(
            Some(GroupDetail(Some(false), Seq(aGroupMember().copy(id = testMemberId)))))
          )

          spyJourneyAction.setReg(reg)

          val resp = controller.addressCaptureCallback(testMemberId)(FakeRequest())

          redirectLocation(resp) mustBe Some(
            routes.AddGroupMemberContactDetailsNameController.displayPage(testMemberId).url
          )

          modifiedRegistration.groupDetail.get.findGroupMember(Some(testMemberId), None)
            .get.addressDetails mustBe validCapturedAddress
          verify(spyJourneyAction).amend
        }
      }
    }
  }
}
