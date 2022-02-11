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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.partner.list_partners_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.FakeRequestCSRFSupport.CSRFFakeRequest

class PartnersListControllerSpec extends ControllerSpec with MockAmendmentJourneyAction {

  private val mcc                  = stubMessagesControllerComponents()
  private val mockListPartnersPage = mock[list_partners_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private val listPartnersController = new PartnersListController(
    authenticate = mockAuthAllowEnrolmentAction,
    amendmentJourneyAction = mockAmendmentJourneyAction,
    mcc = mcc,
    page = mockListPartnersPage
  )

  override protected def beforeEach(): Unit =
    when(
      mockListPartnersPage.apply(any(), ArgumentMatchers.eq(partnershipRegistration))(any(), any())
    ).thenReturn(Html("List Partners Page"))

  "Partners List Controller" should {
    "display partners list page" in {
      authorisedUserWithPptSubscription()
      simulateGetSubscriptionSuccess(partnershipRegistration)

      val resp = listPartnersController.displayPage()(getRequest())
      status(resp) mustBe OK
      contentAsString(resp) mustBe "List Partners Page"
    }

    "reject unselected submission" in {
      authorisedUserWithPptSubscription()
      simulateGetSubscriptionSuccess(partnershipRegistration)

      val resp = listPartnersController.submit()(getSubmissionRequest("addOrganisation" -> ""))
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe "List Partners Page"
    }

    "redirect to the add new partner flow" when {
      "selects to add new partner" in {
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionSuccess(partnershipRegistration)

        val resp = listPartnersController.submit()(getSubmissionRequest("addOrganisation" -> "yes"))
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(
          routes.AddPartnerOrganisationDetailsTypeController.displayPage().url
        )
      }
    }

    "redirect to the manage partners page" when {
      "selects NOT to add new partner" in {
        authorisedUserWithPptSubscription()
        simulateGetSubscriptionSuccess(partnershipRegistration)

        val resp = listPartnersController.submit()(getSubmissionRequest("addOrganisation" -> "no"))
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.ManagePartnersController.displayPage().url)
      }
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

  private def getSubmissionRequest(data: (String, String)) =
    FakeRequest("POST", "").withSession(
      (AmendmentJourneyAction.SessionId, "123")
    ).withFormUrlEncodedBody(data)

}
