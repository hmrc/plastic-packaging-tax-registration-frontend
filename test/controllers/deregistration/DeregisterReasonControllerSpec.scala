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

package controllers.deregistration

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import forms.deregistration.DeregisterReasonForm
import models.deregistration.DeregistrationDetails
import models.deregistration.DeregistrationReason.CeasedTrading
import play.api.test.FakeRequest
import repositories.{DeregistrationDetailRepositoryImpl, DeregistrationDetailsRepository, UserDataRepository}
import views.html.deregistration.deregister_reason_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class DeregisterReasonControllerSpec extends ControllerSpec {
  private val page       = mock[deregister_reason_page]
  private val mcc        = stubMessagesControllerComponents()
  private val mockCache  = mock[UserDataRepository]
  private val repository = new DeregistrationDetailRepositoryImpl(mockCache)

  private val initialDeregistrationDetails =
    DeregistrationDetails(deregister = Some(true), reason = None)

  private val controller =
    new DeregisterReasonController(
      authenticate = FakeAmendAuthAction,
      mcc = mcc,
      deregistrationDetailRepository = repository,
      page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[DeregisterReasonForm]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Confirm Organisation Type Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {

        when(mockCache.getData[DeregistrationDetails](any())(any(), any())).thenReturn(
          Future.successful(Some(initialDeregistrationDetails))
        )

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "user is authorised, details already exist and display page method is invoked" in {

        when(mockCache.getData[DeregistrationDetails](any())(any(), any())).thenReturn(
          Future.successful(Some(initialDeregistrationDetails.copy(reason = Some(CeasedTrading))))
        )
        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "redirect to next page and persist reason" when {
        "a reason is submitted" in {
          val expectedDeregistrationDetails =
            initialDeregistrationDetails.copy(reason = Some(CeasedTrading))
          when(mockCache.putData[DeregistrationDetails](any(), any())(any(), any())).thenReturn(
            Future.successful(expectedDeregistrationDetails)
          )

          val result =
            controller.submit()(postRequestEncoded(DeregisterReasonForm(Some(CeasedTrading))))

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.DeregisterCheckYourAnswersController.displayPage().url)

          verify(mockCache).putData(
            ArgumentMatchers.eq(DeregistrationDetailsRepository.repositoryKey),
            ArgumentMatchers.eq(expectedDeregistrationDetails)
          )(any(), any())
        }
      }

      "redisplay the page with a BAD REQUEST status" when {
        "no reason is submitted" in {

          val result = controller.submit()(postRequestEncoded(JsObject.empty))

          status(result) mustBe BAD_REQUEST
        }
      }

    }

  }
}
