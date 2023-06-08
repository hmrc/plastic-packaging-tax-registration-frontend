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

package controllers.contact

import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import forms.contact.FullName
import models.registration.PrimaryContactDetails
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.contact.full_name_page

class ContactDetailsFullNameControllerSpec extends ControllerSpec {
  private val page = mock[full_name_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsFullNameController(journeyAction = spyJourneyAction,
                                         mockRegistrationConnector,
                                         mcc = mcc,
                                         page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[FullName]], any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Primary Contact Details Full Name Controller" should {

    "return 200" when {
      "user is authorised, a registration already exists and display page method is invoked" in {

        spyJourneyAction.setReg(
          aRegistration(
            withPrimaryContactDetails(PrimaryContactDetails(name = Some("FirstName LastName")))
          )
        )
        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "return 303 (OK) " when {
      "user submits or saves the contact full name" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()

        val result =
          controller.submit()(postRequestEncoded(FullName("FirstName -'. LastName")))

        status(result) mustBe SEE_OTHER

        modifiedRegistration.primaryContactDetails.name mustBe Some("FirstName -'. LastName")


        redirectLocation(result) mustBe Some(
          routes.ContactDetailsJobTitleController.displayPage().url
        )

      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user does not enter name" in {


        val result = controller.submit()(postRequestEncoded(FullName("")))

        status(result) mustBe BAD_REQUEST
      }

      "user enters a long name" in {

        val result = controller.submit()(postRequestEncoded(FullName("abced" * 40)))

        status(result) mustBe BAD_REQUEST
      }

      "user enters non-alphabetic characters" in {

        val result =
          controller.submit()(
            postRequestEncoded(FullName("FirstNam807980234£$ LastName"))
          )

        status(result) mustBe BAD_REQUEST
      }

      "user enters non-alphabetic no digits" in {

        val result =
          controller.submit()(postRequestEncoded(FullName("()/,& LastName")))

        status(result) mustBe BAD_REQUEST
      }

      "user enters accented characters" in {

        val result =
          controller.submit()(postRequestEncoded(FullName("Chlöe Anne-Marie")))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user submits form and the registration update fails" in {

        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError](status(
          controller.submit()(postRequestEncoded(FullName("FirstName LastName")))
        ))
      }

      "user submits form and a registration update runtime exception occurs" in {

        mockRegistrationException()

        intercept[RuntimeException](status(
          controller.submit()(postRequestEncoded(FullName("FirstName LastName")))
        ))
      }
    }

    "display page for a group organisation" in {

      spyJourneyAction.setReg(
        aRegistration(
          withPrimaryContactDetails(PrimaryContactDetails(name = Some("FirstName LastName"))),
          withGroupDetail(Some(groupDetailsWithMembers))
        )
      )

      await(controller.displayPage()(FakeRequest()))

      val captor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(page).apply(any(), any(), captor.capture())(any(), any())
      captor.getValue mustBe true
    }
  }
}
