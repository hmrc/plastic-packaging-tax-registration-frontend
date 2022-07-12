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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group

import base.unit.{AddressCaptureSpec, ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address.UKAddress
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.confirm_business_address
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class AddGroupMemberConfirmBusinessAddressControllerSpec extends ControllerSpec with AddressCaptureSpec
  with MockAmendmentJourneyAction with PptTestData {

  private val page = mock[confirm_business_address]
  private val mcc = stubMessagesControllerComponents()

  private val controller =
    new AddGroupMemberConfirmBusinessAddressController(
      authenticate = mockEnrolledAuthAction,
      journeyAction = mockAmendmentJourneyAction,
      registrationConnector = mockRegistrationConnector,
      mockAddressCaptureService,
      mcc = mcc,
      page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    inMemoryRegistrationAmendmentRepository.reset()
    when(page.apply(any[Address], any(), any())(any(), any())).thenReturn(HtmlFormat.raw("business registered address"))
    authorisedUserWithPptSubscription()
    mockRegistrationUpdate()
    simulateSuccessfulAddressCaptureInit(None)
    simulateValidAddressCapture()
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  val getRequestWithSession: Request[AnyContentAsEmpty.type] = getRequest("sessionId" -> "123")

  val testMemberId = "dfd64d98-9274-496b-9801-5390da22660e"

  ".displayPage" should {

    "display business address for confirmation when it is populated and valid" in {

      val reg = aRegistration(withGroupDetail(
        Some(GroupDetail(Some(false), Seq(aGroupMember().copy(id = testMemberId)))))
      )

      inMemoryRegistrationAmendmentRepository.put("123", reg)

      val resp = controller.displayPage(testMemberId)(getRequestWithSession)

      status(resp) mustBe OK

      contentAsString(resp) mustBe "business registered address"
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

        inMemoryRegistrationAmendmentRepository.put("123", reg)

        val resp = controller.displayPage(testMemberId)(getRequestWithSession)

        redirectLocation(resp) mustBe Some(addressCaptureRedirect.url)
      }
    }

    ".addressCaptureCallback" when {
      "address is retrieved from address capture service, reg is updated" should {
        "redirect to group member contact name" in {

          val reg = aRegistration(withGroupDetail(
            Some(GroupDetail(Some(false), Seq(aGroupMember().copy(id = testMemberId)))))
          )

          inMemoryRegistrationAmendmentRepository.put("123", reg)

          val resp = controller.addressCaptureCallback(testMemberId)(getRequestWithSession)

          redirectLocation(resp) mustBe Some(
            routes.AddGroupMemberContactDetailsNameController.displayPage(testMemberId).url
          )

          modifiedRegistration.groupDetail.get.findGroupMember(Some(testMemberId), None)
            .get.addressDetails mustBe validCapturedAddress
        }
      }
    }
  }
}
