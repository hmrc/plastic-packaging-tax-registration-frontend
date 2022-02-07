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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment

import base.PptTestData.newUser
import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status.OK
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.amend_registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partials.amendment.amend_error_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

class AmendRegistrationControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction with TableDrivenPropertyChecks {

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
    new AmendRegistrationController(mockAuthAllowEnrolmentAction,
                                    mcc,
                                    mockAmendmentJourneyAction,
                                    amendRegistrationPage,
                                    amendRegistrationErrorPage
    )

  private val populatedRegistration = aRegistration()

  override protected def beforeEach(): Unit = {
    inMemoryRegistrationAmendmentRepository.reset()
    simulateGetSubscriptionSuccess(populatedRegistration)
  }

  "Amend Registration Controller" should {

    "display the amend registration page" when {

      "user is authenticated" in {
        authorisedUserWithPptSubscription()

        val resp = controller.displayPage()(getRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "registration amendment"
      }
    }

    "display the amend error page" in {
      authorizedUser()

      val resp = controller.registrationUpdateFailed()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "registration amendment error"
    }

    "throw exception" when {
      "unauthenticated user attempts to access the amendment page" in {
        unAuthorizedUser()

        val resp = controller.displayPage()(getRequest())

        intercept[RuntimeException] {
          status(resp)
        }
      }

      "user is authenticated but attempt to retrieve registration via connector fails" in {
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionFailure()

        val resp = controller.displayPage()(getRequest())

        intercept[RuntimeException] {
          status(resp)
        }
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
