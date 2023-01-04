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

package controllers.enrolment

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import forms.enrolment.PptReference
import models.registration.UserEnrolmentDetails
import repositories.{
  UserDataRepository,
  UserEnrolmentDetailsRepository
}
import views.html.enrolment.ppt_reference_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class PptReferenceControllerSpec extends ControllerSpec {

  private val page       = mock[ppt_reference_page]
  private val mcc        = stubMessagesControllerComponents()
  private val mockCache  = mock[UserDataRepository]
  private val repository = new UserEnrolmentDetailsRepository(mockCache)

  private val controller = new PptReferenceController(mockAuthAction, mcc, repository, page)

  val pptReference     = PptReference("XAPPT0001234567")
  val enrolmentDetails = UserEnrolmentDetails(pptReference = Some(pptReference))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[PptReference]])(any(), any())).thenReturn(
      HtmlFormat.raw("PPT Reference Page")
    )
    when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
      Future.successful(Some(enrolmentDetails))
    )
  }

  override protected def afterEach(): Unit = {
    reset(page, mockCache)
    super.afterEach()
  }

  "PPT Reference Controller" should {
    "display the ppt reference page" when {
      "user is authorised" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "PPT Reference Page"
      }

      "user is authorised and cache is empty" in {
        authorizedUser()
        when(mockCache.getData[UserEnrolmentDetails](any())(any(), any())).thenReturn(
          Future.successful(None)
        )
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "PPT Reference Page"
      }
    }
    "throw a RuntimeException" when {
      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    "redisplay the ppt reference page with a BAD REQUEST status" when {
      "an invalid ppt reference is submitted" in {
        authorizedUser()
        val result = controller.submit()(postRequestEncoded(PptReference("XXX")))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "PPT Reference Page"
      }
    }

    "redirect to next page and persist ppt reference" when {
      "a valid ppt reference is submitted" in {

        when(mockCache.putData[UserEnrolmentDetails](any(), any())(any(), any())).thenReturn(
          Future.successful(enrolmentDetails)
        )

        authorizedUser()
        val result = controller.submit()(postRequestEncoded(pptReference))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.VerifyOrganisationController.displayPage().url)

        verify(mockCache).putData(ArgumentMatchers.eq(UserEnrolmentDetailsRepository.repositoryKey),
                                  ArgumentMatchers.eq(enrolmentDetails)
        )(any(), any())
      }
    }
  }
}
