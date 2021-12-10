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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment

import base.PptTestData.newUser
import base.unit.MockAmendmentJourneyAction
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.http.Status.OK
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.Helpers.{contentAsString, status}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.plasticpackagingtax.registration.models.enrolment.PptEnrolment
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.amend_registration_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

class AmendRegistrationControllerTest
    extends MockAmendmentJourneyAction with Matchers with DefaultAwaitTimeout {

  private val mcc  = stubMessagesControllerComponents()
  private val page = mock[amend_registration_page]

  when(page.apply(any())(any(), any())).thenReturn(HtmlFormat.raw("registration amendment"))

  private val controller =
    new AmendRegistrationController(mockAuthAllowEnrolmentAction,
                                    mcc,
                                    mockAmendmentJourneyAction,
                                    page
    )

  private def authorisedUserWithPptSubscription() =
    authorizedUser(user =
      newUser().copy(enrolments =
        Enrolments(
          Set(
            new Enrolment(PptEnrolment.Identifier,
                          Seq(EnrolmentIdentifier(PptEnrolment.Key, "XMPPT0000000123")),
                          "activated"
            )
          )
        )
      )
    )

  "Amend Registration Controller" should {

    "display the amend registration page" when {

      "user is authenticated" in {
        authorisedUserWithPptSubscription()

        val resp = controller.displayPage()(getRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "registration amendment"
      }
    }

    "throw exception" when {
      "unauthenticated user attempts to access the amendment page" in {
        unAuthorizedUser()

        val resp = controller.displayPage()(getRequest())

        intercept[RuntimeException] {
          status(resp)
        }
      }

      "user is authenticated but attempt to retrieve registration fails" in {
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionFailure()

        val resp = controller.displayPage()(getRequest())

        intercept[RuntimeException] {
          status(resp)
        }
      }
    }
  }

  private def getRequest(session: (String, String) = "" -> ""): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession(session).withCSRFToken

}
