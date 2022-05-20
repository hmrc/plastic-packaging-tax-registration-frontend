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
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.Partner
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.partner.confirm_remove_partner_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.plasticpackagingtax.registration.utils.FakeRequestCSRFSupport._

class ConfirmRemovePartnerControllerSpec extends ControllerSpec with MockAmendmentJourneyAction {

  private val sessionId = "123"

  private val mcc                          = stubMessagesControllerComponents()
  private val mockConfirmRemovePartnerPage = mock[confirm_remove_partner_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private val confirmRemovePartnerController = new ConfirmRemovePartnerController(
    authenticate = mockEnrolledAuthAction,
    amendmentJourneyAction = mockAmendmentJourneyAction,
    mcc = mcc,
    page = mockConfirmRemovePartnerPage
  )

  when(mockConfirmRemovePartnerPage.apply(any(), any())(any(), any())).thenAnswer { inv =>
    Html(s"Are you sure you want to remove ${inv.getArgument[Partner](1).id} from the partnership?")
  }

  override protected def beforeEach(): Unit = {
    authorisedUserWithPptSubscription()
    simulateGetSubscriptionSuccess(partnershipRegistration)
  }

  "Confirm Remove Partner Controller" should {
    "display the confirm remove partner page with the correct partner details" in {
      val resp = confirmRemovePartnerController.displayPage(
        partnershipRegistration.otherPartners.head.id
      )(getRequest())
      status(resp) mustBe OK
      contentAsString(resp) contains partnershipRegistration.otherPartners.head.name
    }

    "throw IllegalStateException if the specified partner cannot be found" in {
      intercept[IllegalStateException] {
        await(confirmRemovePartnerController.displayPage("XXX")(getRequest()))
      }
    }

    "reject unselected submission and redisplay page" in {
      val resp = confirmRemovePartnerController.submit(
        partnershipRegistration.otherPartners.head.id
      )(getSubmissionRequest("value" -> ""))
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) contains "Are you sure you want to remove"
    }

    "redirect to the partner list page" when {
      "the user opts NOT to remove the partner" in {
        val resp = confirmRemovePartnerController.submit(
          partnershipRegistration.otherPartners.head.id
        )(getSubmissionRequest("value" -> "no"))
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.PartnersListController.displayPage().url)
      }
    }

    "remove the partner and redirect to the partner list page" when {
      "the user opts to remove the partner" in {
        simulateUpdateSubscriptionSuccess()

        val resp = confirmRemovePartnerController.submit(
          partnershipRegistration.otherPartners.head.id
        )(getSubmissionRequest("value" -> "yes"))
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.PartnersListController.displayPage().url)

        verifySubscriptionUpdateWasSubmittedToETMP { registration =>
          !registration.otherPartners.map(_.id).contains(
            partnershipRegistration.otherPartners.head.id
          )
        }

        val updatedReg = await(inMemoryRegistrationAmendmentRepository.get(sessionId)).get
        updatedReg.otherPartners.map(_.id).contains(
          partnershipRegistration.otherPartners.head.id
        ) mustBe false
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

  private def getSubmissionRequest(data: (String, String)) =
    FakeRequest("POST", "").withSession(
      (AmendmentJourneyAction.SessionId, sessionId)
    ).withFormUrlEncodedBody(data)

  private def verifySubscriptionUpdateWasSubmittedToETMP(
    registrationCheck: Registration => Boolean
  ) = {
    val registrationCaptor: ArgumentCaptor[Registration] =
      ArgumentCaptor.forClass(classOf[Registration])
    verify(mockSubscriptionConnector).updateSubscription(any(), registrationCaptor.capture())(any())

    registrationCheck(registrationCaptor.getValue) mustBe true
  }

}
