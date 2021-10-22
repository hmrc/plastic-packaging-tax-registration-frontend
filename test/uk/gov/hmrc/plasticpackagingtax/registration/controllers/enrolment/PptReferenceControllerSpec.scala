/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.PptReference
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.PublicBodyRegistration
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.{
  PublicBodyRegistrationRepository,
  UserDataRepository
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.ppt_reference_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class PptReferenceControllerSpec extends ControllerSpec {

  private val page       = mock[ppt_reference_page]
  private val mcc        = stubMessagesControllerComponents()
  private val mockCache  = mock[UserDataRepository]
  private val repository = new PublicBodyRegistrationRepository(mockCache)

  private val controller = new PptReferenceController(mockAuthAction, mcc, repository, page)

  val pptReference = PptReference("XAPPT000123456")
  val registration = PublicBodyRegistration(pptReference = Some(pptReference))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[PptReference]])(any(), any())).thenReturn(
      HtmlFormat.raw("PPT Reference Page")
    )
    when(mockCache.getData[PublicBodyRegistration](any())(any(), any())).thenReturn(
      Future.successful(Some(registration))
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

    "redirect to next page" when {
      "a valid ppt reference is submitted" in {

        when(mockCache.putData[PublicBodyRegistration](any(), any())(any(), any())).thenReturn(
          Future.successful(registration)
        )

        authorizedUser()
        val result = controller.submit()(postRequestEncoded(pptReference))

        status(result) mustBe SEE_OTHER

        // TODO: this will need to change when we build out the next enrolment page
        redirectLocation(result) mustBe Some(routes.PptReferenceController.displayPage().url)

        verify(mockCache).putData(ArgumentMatchers.eq("publicBodyRegistration"),
                                  ArgumentMatchers.eq(registration)
        )(any(), any())
      }
    }
  }
}
