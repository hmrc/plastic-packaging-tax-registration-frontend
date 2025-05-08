/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.amendment.partner.list_partners_page

class PartnersListControllerSpec extends ControllerSpec with AmendmentControllerSpec {

  private val mcc                  = stubMessagesControllerComponents()
  private val mockListPartnersPage = mock[list_partners_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private val listPartnersController =
    new PartnersListController(mcc = mcc, page = mockListPartnersPage, journeyAction = spyJourneyAction)

  override protected def beforeEach(): Unit =
    when(mockListPartnersPage.apply(any(), ArgumentMatchers.eq(partnershipRegistration))(any(), any())).thenReturn(
      Html("List Partners Page")
    )

  "Partners List Controller" should {
    "display partners list page" in {
      spyJourneyAction.setReg(partnershipRegistration)

      val resp = listPartnersController.displayPage()(FakeRequest())
      status(resp) mustBe OK
      contentAsString(resp) mustBe "List Partners Page"
    }

    "reject unselected submission and redisplay page" in {
      spyJourneyAction.setReg(partnershipRegistration)

      val resp = listPartnersController.submit()(FakeRequest().withFormUrlEncodedBody("addOrganisation" -> ""))
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) mustBe "List Partners Page"
    }

    "redirect to the add new partner flow" when {
      "selects to add new partner" in {
        spyJourneyAction.setReg(partnershipRegistration)

        val resp = listPartnersController.submit()(postRequest.withFormUrlEncodedBody("addOrganisation" -> "yes"))
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.AddPartnerOrganisationDetailsTypeController.displayPage().url)
      }
    }

    "redirect to the manage partners page" when {
      "selects NOT to add new partner" in {
        spyJourneyAction.setReg(partnershipRegistration)

        val resp = listPartnersController.submit()(postRequest.withFormUrlEncodedBody("addOrganisation" -> "no"))
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.ManagePartnersController.displayPage().url)
      }
    }
  }

}
