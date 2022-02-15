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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.partner

import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.{routes => amendRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.partner.amend_add_partner_contact_check_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

class AddPartnerContactDetailsCheckAnswersControllerSpec
    extends ControllerSpec with MockAmendmentJourneyAction {

  private val mcc     = stubMessagesControllerComponents()
  private val cyaPage = mock[amend_add_partner_contact_check_answers_page]

  when(cyaPage.apply(any())(any(), any())).thenReturn(HtmlFormat.raw("Amend Reg - New Partner CYA"))

  private val controller = new AddPartnerContactDetailsCheckAnswersController(
    authenticate = mockAuthAllowEnrolmentAction,
    journeyAction = mockAmendmentJourneyAction,
    mcc = mcc,
    page = cyaPage
  )

  private val partnerRegistrationInAmendment = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  override protected def beforeEach(): Unit = {
    inMemoryRegistrationAmendmentRepository.reset()
    reset(mockSubscriptionConnector)
    simulateGetSubscriptionSuccess(partnerRegistrationInAmendment)
    simulateUpdateSubscriptionSuccess()
  }

  "AddPartnerContactDetailsCheckAnswersController" when {

    "displaying page" should {
      "display successfully" when {
        "partner present" in {
          authorisedUserWithPptSubscription()

          val resp = controller.displayPage()(getRequest())

          status(resp) mustBe OK
          contentAsString(resp) mustBe "Amend Reg - New Partner CYA"
        }
      }
    }

    "details confirmed" should {
      "update subscription at the ETMP back end" in {
        authorisedUserWithPptSubscription()

        await(controller.submit()(postRequest(JsObject.empty)))

        val registrationCaptor: ArgumentCaptor[Registration] =
          ArgumentCaptor.forClass(classOf[Registration])
        verify(mockSubscriptionConnector).updateSubscription(any(), registrationCaptor.capture())(
          any()
        )

        registrationCaptor.getValue mustBe partnerRegistrationInAmendment
      }

      "redirect to the manage group page when update successful" in {
        authorisedUserWithPptSubscription()

        val resp = controller.submit()(postRequest(JsObject.empty))

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.ManagePartnersController.displayPage().toString)
      }
      "redirect to the post reg amend error page" when {
        "update fails due to exception being thrown" in {
          authorisedUserWithPptSubscription()
          simulateUpdateSubscriptionFailure(new RuntimeException("BANG!"))

          val resp = controller.submit()(postRequest(JsObject.empty))

          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some(
            amendRoutes.AmendRegistrationController.registrationUpdateFailed().toString
          )
        }
        "update fails due to error returned from ETMP" in {
          authorisedUserWithPptSubscription()
          simulateUpdateSubscriptionFailureReturnedError()

          val resp = controller.submit()(postRequest(JsObject.empty))

          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some(
            amendRoutes.AmendRegistrationController.registrationUpdateFailed().toString
          )
        }
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
