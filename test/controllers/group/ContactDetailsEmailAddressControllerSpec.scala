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

import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import controllers.group.{routes => groupRoutes}
import forms.contact.EmailAddress
import models.registration.NewRegistrationUpdateService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.group.member_email_address_page

class ContactDetailsEmailAddressControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[member_email_address_page]
  private val mcc  = stubMessagesControllerComponents()

  private val mockNewRegistrationUpdater = new NewRegistrationUpdateService(
    mockRegistrationConnector
  )

  private val controller =
    new ContactDetailsEmailAddressController(journeyAction = spyJourneyAction,
                                             mcc = mcc,
                                             page = page,
                                             registrationUpdater = mockNewRegistrationUpdater
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "ContactDetailsEmailAddressController" should {

    "return 200" when {
      "user is authorised display page method is invoked" in {

        val member = groupMember.copy(contactDetails = None)
        spyJourneyAction.setReg(
          aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(member)))))
        )
        val result = controller.displayPage(groupMember.id)(FakeRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {

        spyJourneyAction.setReg(
          aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember)))))
        )
        val result = controller.displayPage(groupMember.id)(FakeRequest())

        status(result) mustBe OK
      }
    }

      "return 303 (OK)" when {
        "user submits the email" in {

          spyJourneyAction.setReg(
            aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember)))))
          )
          mockRegistrationUpdate()

          val result =
            controller.submit(groupMember.id)(
              postRequestEncoded(EmailAddress("test@test.com"))
            )

          status(result) mustBe SEE_OTHER
          modifiedRegistration.groupDetail.get.members.lastOption.get.contactDetails.get.email mustBe Some(
            "test@test.com"
          )
          redirectLocation(result) mustBe Some(
            groupRoutes.ContactDetailsTelephoneNumberController.displayPage(groupMember.id).url
          )
          reset(mockRegistrationConnector)
        }
      }
    }

    "return pre populated form" when {

      def pageForm: Form[EmailAddress] = {
        val captor = ArgumentCaptor.forClass(classOf[Form[EmailAddress]])
        verify(page).apply(captor.capture(), any(), any())(any(), any())
        captor.getValue
      }

      "data exist" in {

        spyJourneyAction.setReg(
          aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember)))))
        )

        await(controller.displayPage(groupMember.id)(FakeRequest()))

        pageForm.get.value mustBe "test@test.com"
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid email" in {

        val result =
          controller.submit(groupMember.id)(postRequest(Json.toJson(EmailAddress("$%^"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user submits form and the registration update fails" in {

        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError](status(
          controller.submit(groupMember.id)(postRequestEncoded(EmailAddress("test@test.com")))
        ))
      }

      "user submits form and a registration update runtime exception occurs" in {

        mockRegistrationException()

        intercept[RuntimeException](status(
          controller.submit(groupMember.id)(postRequestEncoded(EmailAddress("test@test.com")))
        ))
      }

  }

}
