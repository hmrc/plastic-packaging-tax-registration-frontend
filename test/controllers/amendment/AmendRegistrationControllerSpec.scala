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

package controllers.amendment

import base.unit.{AmendmentControllerSpec, ControllerSpec}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.amendment.amend_registration_page
import views.html.partials.amendment.amend_error_page

class AmendRegistrationControllerSpec
    extends ControllerSpec with AmendmentControllerSpec with TableDrivenPropertyChecks {

  private val mcc = stubMessagesControllerComponents()

  private val amendRegistrationPage      = mock[amend_registration_page]
  private val amendRegistrationErrorPage = mock[amend_error_page]

  when(amendRegistrationPage.apply(any())(any(), any())).thenReturn(
    HtmlFormat.raw("registration amendment")
  )

  when(amendRegistrationErrorPage.apply()(any(), any())).thenReturn(
    HtmlFormat.raw("registration amendment error")
  )

  private val controller =
    new AmendRegistrationController(spyJourneyAction,
      mockAmendRegService,
      FakeAmendAuthAction,
      mcc,
      amendRegistrationPage,
      amendRegistrationErrorPage
    )

  private val populatedRegistration = aRegistration()

  override protected def beforeEach(): Unit = {
    inMemoryRegistrationAmendmentRepository.reset()
    spyJourneyAction.setReg(populatedRegistration)
  }

  "Amend Registration Controller" should {

    "display the amend registration page" when {

      "user is authenticated" in {
        val resp = controller.displayPage()(FakeRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "registration amendment"
      }
    }

    "display the amend error page" in {


      val resp = controller.registrationUpdateFailed()(FakeRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "registration amendment error"
    }
  }

}
