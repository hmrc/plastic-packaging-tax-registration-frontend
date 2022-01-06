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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.member_email_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsEmailAddressControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[member_email_address_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsEmailAddressController(authenticate = mockAuthAction,
                                             journeyAction = mockJourneyAction,
                                             registrationConnector = mockRegistrationConnector,
                                             mcc = mcc,
                                             page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "ContactDetailsEmailAddressController" should {

    "return 200" when {
      "user is authorised display page method is invoked" in {
        authorizedUser()
        val member = groupMember.copy(contactDetails = None)
        mockRegistrationFind(
          aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(member)))))
        )
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember)))))
        )
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    forAll(Seq(saveAndContinueFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {
        "user submits the email" in {
          authorizedUser()
          mockRegistrationFind(
            aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember)))))
          )
          mockRegistrationUpdate()

          val result =
            controller.submit()(postRequestEncoded(EmailAddress("test@test.com"), formAction))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.groupDetail.get.members.lastOption.get.contactDetails.get.email mustBe Some(
            "test@test.com"
          )
          redirectLocation(result) mustBe Some(
            groupRoutes.ContactDetailsTelephoneNumberController.displayPage().url
          )
          reset(mockRegistrationConnector)
        }
      }
    }

    "return pre populated form" when {

      def pageForm: Form[EmailAddress] = {
        val captor = ArgumentCaptor.forClass(classOf[Form[EmailAddress]])
        verify(page).apply(captor.capture(), any(), any(), any())(any(), any())
        captor.getValue
      }

      "data exist" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(withGroupDetail(Some(groupDetails.copy(members = Seq(groupMember)))))
        )

        await(controller.displayPage()(getRequest()))

        pageForm.get.value mustBe "test@test.com"
      }
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid phone number" in {
        authorizedUser()
        val result = controller.submit()(postRequest(Json.toJson(EmailAddress("$%^"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationUpdateFailure()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[IllegalStateException](status(result))
      }

      "user submits form and a registration update runtime exception occurs" in {
        authorizedUser()
        mockRegistrationException()
        val result =
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[RuntimeException](status(result))
      }
    }
  }

}
