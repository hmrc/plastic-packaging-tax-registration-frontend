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

package controllers.group

import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import controllers.group.{routes => groupRoutes}
import forms.group.MemberName
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
import views.html.group.member_name_page

class ContactDetailsNameControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[member_name_page]
  private val mcc  = stubMessagesControllerComponents()

  private val mockNewRegistrationUpdater = new NewRegistrationUpdateService(
    mockRegistrationConnector
  )

  private val controller =
    new ContactDetailsNameController(journeyAction = spyJourneyAction,
                                     mcc = mcc,
                                     page = page,
                                     registrationUpdater = mockNewRegistrationUpdater
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "ContactDetailsNameController" should {

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
        "user submits the member name" in {

          spyJourneyAction.setReg(
            aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember)))))
          )
          mockRegistrationUpdate()

          val result =
            controller.submit(groupMember.id)(
              postRequestEncoded(MemberName(firstName = "Test -'.", lastName = "User -'."))
            )

          status(result) mustBe SEE_OTHER
          val contactDetails =
            modifiedRegistration.groupDetail.get.members.lastOption.get.contactDetails.get
          contactDetails.firstName mustBe "Test -'."
          contactDetails.lastName mustBe "User -'."
          redirectLocation(result) mustBe Some(
            groupRoutes.ContactDetailsEmailAddressController.displayPage(groupMember.id).url
          )
          reset(mockRegistrationConnector)
        }
      }
    }

    "return pre populated form" when {

      def pageForm: Form[MemberName] = {
        val captor = ArgumentCaptor.forClass(classOf[Form[MemberName]])
        verify(page).apply(captor.capture(), any(), any(), any())(any(), any())
        captor.getValue
      }

      "data exist" in {

        spyJourneyAction.setReg(
          aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember)))))
        )

        await(controller.displayPage(groupMember.id)(FakeRequest()))

        pageForm.get.firstName mustBe "Test"
        pageForm.get.lastName mustBe "User"
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits numbers in firstname" in {

        val result =
          controller.submit(groupMember.id)(postRequest(Json.toJson(MemberName("123", "andy"))))

        status(result) mustBe BAD_REQUEST
      }
      "user submits numbers in lastname" in {

        val result =
          controller.submit(groupMember.id)(postRequest(Json.toJson(MemberName("andy", "123"))))

        status(result) mustBe BAD_REQUEST
      }

      "user submits accented member name" in {

        val result =
          controller.submit(groupMember.id)(postRequest(Json.toJson(MemberName("Chlöe", "Bòb"))))

        status(result) mustBe BAD_REQUEST
      }

      "user submits invalid member name" in {

        val result =
          controller.submit(groupMember.id)(postRequest(Json.toJson(MemberName("$%^", "£$£¬"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user submits form and the registration update fails" in {

        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError](status(
          controller.submit(groupMember.id)(
            postRequestEncoded(MemberName(firstName = "Test", lastName = "User"))
          )
        ))
      }

      "user submits form and a registration update runtime exception occurs" in {

        mockRegistrationException()

        intercept[RuntimeException](status(
          controller.submit(groupMember.id)(
            postRequestEncoded(MemberName(firstName = "Test", lastName = "User"))
          )
        ))
      }

  }

}
