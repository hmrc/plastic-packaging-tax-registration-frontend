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
import connectors.DownstreamServiceError
import controllers.{routes => pptRoutes}
import forms.contact.JobTitle
import models.registration.PrimaryContactDetails
import views.html.contact.job_title_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsJobTitleControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[job_title_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsJobTitleController(journeyAction = spyJourneyAction,
                                         registrationConnector = mockRegistrationConnector,
                                         mcc = mcc,
                                         page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "ContactDetailsJobTitleController" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        spyJourneyAction.setReg(aRegistration())

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

    }

      "return 303 (OK)" when {
        "user submits the job title" in {

          spyJourneyAction.setReg(aRegistration())
          mockRegistrationUpdate()

          val result =
            controller.submit()(postRequestEncoded(JobTitle("tester")))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.primaryContactDetails.jobTitle mustBe Some("tester")


          redirectLocation(result) mustBe Some(
            routes.ContactDetailsEmailAddressController.displayPage().url
          )

          reset(mockRegistrationConnector)
        }
      }
    }

    "return prepopulated form" when {

      def pageForm: Form[JobTitle] = {
        val captor = ArgumentCaptor.forClass(classOf[Form[JobTitle]])
        verify(page).apply(captor.capture(), any(), any())(any(), any())
        captor.getValue
      }

      "data exist" in {

        spyJourneyAction.setReg(
          aRegistration(withPrimaryContactDetails(PrimaryContactDetails(jobTitle = Some("tester"))))
        )

        await(controller.displayPage()(getRequest()))

        pageForm.get.value mustBe "tester"
      }
    }

    "display page for a group organisation" in {

      spyJourneyAction.setReg(
        aRegistration(
          withPrimaryContactDetails(PrimaryContactDetails(name = Some("FirstName LastName"))),
          withGroupDetail(Some(groupDetailsWithMembers))
        )
      )

      await(controller.displayPage()(getRequest()))

      val captor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(page).apply(any(), any(), captor.capture())(any(), any())
      captor.getValue mustBe true
    }

    "return 400 (BAD_REQUEST)" when {

      "user submits invalid job title" in {

        val result =
          controller.submit()(postRequest(Json.toJson(JobTitle("$%^"))))

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user submits form and the registration update fails" in {

        mockRegistrationUpdateFailure()

        intercept[DownstreamServiceError](status(
          controller.submit()(postRequest(Json.toJson(JobTitle("tester"))))
        ))
      }

      "user submits form and a registration update runtime exception occurs" in {

        mockRegistrationException()

        intercept[RuntimeException](status(
          controller.submit()(postRequest(Json.toJson(JobTitle("tester"))))
        ))
      }
    
  }
}
