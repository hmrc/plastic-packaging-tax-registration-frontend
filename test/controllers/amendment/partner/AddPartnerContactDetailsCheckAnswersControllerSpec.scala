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

package controllers.amendment.partner

import base.unit.{AmendmentControllerSpec, ControllerSpec}
import controllers.amendment.{routes => amendRoutes}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.amendment.partner.amend_add_partner_contact_check_answers_page

class AddPartnerContactDetailsCheckAnswersControllerSpec
    extends ControllerSpec with AmendmentControllerSpec {

  private val mcc     = stubMessagesControllerComponents()
  private val cyaPage = mock[amend_add_partner_contact_check_answers_page]

  when(cyaPage.apply(any())(any(), any())).thenReturn(HtmlFormat.raw("Amend Reg - New Partner CYA"))

  private val controller = new AddPartnerContactDetailsCheckAnswersController(
    amendRegistrationService = mockAmendRegService,
    journeyAction = spyJourneyAction,
    mcc = mcc,
    page = cyaPage
  )

  private val partnerRegistrationInAmendment = aRegistration(
    withPartnershipDetails(
      Some(generalPartnershipDetails.copy(inflightPartner = Some(aSoleTraderPartner)))
    )
  )

  override protected def beforeEach(): Unit = {
    reset(mockAmendRegService, spyJourneyAction)
    spyJourneyAction.reset()
    spyJourneyAction.setReg(partnerRegistrationInAmendment)
    simulateUpdateWithRegSubscriptionSuccess()
  }

  "AddPartnerContactDetailsCheckAnswersController" when {

    "displaying page" should {
      "display successfully" when {
        "partner present" in {
          spyJourneyAction.setReg(partnerRegistrationInAmendment)
          val resp = controller.displayPage()(FakeRequest())

          status(resp) mustBe OK
          contentAsString(resp) mustBe "Amend Reg - New Partner CYA"
        }
      }
    }

    "details confirmed" should {
      "update subscription at the ETMP back end" in {

        await(controller.submit()(postRequest(JsObject.empty)))

        val updated = getUpdatedRegistrationMethod()(partnerRegistrationInAmendment)

        updated.inflightPartner mustBe None
        updated.newPartner mustBe Some(aSoleTraderPartner)
      }

      "redirect to the manage group page when update successful" in {

        val resp = controller.submit()(postRequest(JsObject.empty))

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.ManagePartnersController.displayPage().toString)
      }
      "redirect to the post reg amend error page" when {
        "update fails due to exception being thrown" in {
          simulateUpdateWithRegSubscriptionFailure(new RuntimeException("BANG!"))

          val resp = controller.submit()(postRequest(JsObject.empty))

          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some(
            amendRoutes.AmendRegistrationController.registrationUpdateFailed().toString
          )
        }
        "update fails due to error returned from ETMP" in {
          simulateUpdateSubscriptionWithRegFailureReturnedError()

          val resp = controller.submit()(postRequest(JsObject.empty))

          status(resp) mustBe SEE_OTHER
          redirectLocation(resp) mustBe Some(
            amendRoutes.AmendRegistrationController.registrationUpdateFailed().toString
          )
        }
      }
    }
  }

}
