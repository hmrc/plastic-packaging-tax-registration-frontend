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
import models.genericregistration.Partner
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Play.materializer
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status, POST}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.amendment.partner.confirm_remove_partner_page

class ConfirmRemovePartnerControllerSpec extends ControllerSpec with AmendmentControllerSpec {

  private val mcc                          = stubMessagesControllerComponents()
  private val mockConfirmRemovePartnerPage = mock[confirm_remove_partner_page]

  private val partnershipRegistration = aRegistration(withPartnershipDetails(Some(generalPartnershipDetailsWithPartners)))

  private val confirmRemovePartnerController =
    new ConfirmRemovePartnerController(mcc = mcc, page = mockConfirmRemovePartnerPage, journeyAction = spyJourneyAction, amendRegistrationService = mockAmendRegService)

  when(mockConfirmRemovePartnerPage.apply(any(), any())(any(), any())).thenAnswer { inv =>
    Html(s"Are you sure you want to remove ${inv.getArgument[Partner](1).id} from the partnership?")
  }

  override protected def beforeEach(): Unit =
    spyJourneyAction.setReg(partnershipRegistration)

  "Confirm Remove Partner Controller" should {
    "display the confirm remove partner page with the correct partner details" in {
      val resp = confirmRemovePartnerController.displayPage(partnershipRegistration.otherPartners.head.id)(FakeRequest())
      status(resp) mustBe OK
      contentAsString(resp) contains partnershipRegistration.otherPartners.head.name
    }

    "throw IllegalStateException if the specified partner cannot be found" in {
      intercept[IllegalStateException] {
        await(confirmRemovePartnerController.displayPage("XXX")(FakeRequest()))
      }
    }

    "reject unselected submission and redisplay page" in {
      val resp = confirmRemovePartnerController.submit(partnershipRegistration.otherPartners.head.id)(FakeRequest().withFormUrlEncodedBody("value" -> ""))
      status(resp) mustBe BAD_REQUEST
      contentAsString(resp) contains "Are you sure you want to remove"
    }

    "redirect to the partner list page" when {
      "the user opts NOT to remove the partner" in {
        val resp = confirmRemovePartnerController.submit(partnershipRegistration.otherPartners.head.id)(FakeRequest(POST, "").withFormUrlEncodedBody("value" -> "no"))
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.PartnersListController.displayPage().url)
      }
    }

    "remove the partner and redirect to the partner list page" when {
      "the user opts to remove the partner" in {
        simulateUpdateWithRegSubscriptionSuccess()

        val resp = confirmRemovePartnerController.submit(partnershipRegistration.otherPartners.head.id)(FakeRequest(POST, "").withFormUrlEncodedBody("value" -> "yes"))
        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.PartnersListController.displayPage().url)

        val updatedReg = getUpdatedRegistrationMethod().apply(partnershipRegistration)
        updatedReg.otherPartners.map(_.id).contains(partnershipRegistration.otherPartners.head.id) mustBe false
      }
    }
  }

}
