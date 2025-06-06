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
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.amendment.partner.amend_partner_contact_check_answers_page

class PartnerContactDetailsCheckAnswersControllerSpec extends ControllerSpec with AmendmentControllerSpec {

  private val mcc                 = stubMessagesControllerComponents()
  private val mockPartnerCYAsPage = mock[amend_partner_contact_check_answers_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private val controller =
    new PartnerContactDetailsCheckAnswersController(
      mcc = mcc,
      page = mockPartnerCYAsPage,
      journeyAction = spyJourneyAction,
      amendRegistrationService = mockAmendRegService
    )

  override protected def beforeEach(): Unit = {
    spyJourneyAction.setReg(partnershipRegistration)

    when(
      mockPartnerCYAsPage.apply(ArgumentMatchers.eq(partnershipRegistration.nominatedPartner.get))(any(), any())
    ).thenReturn(Html("Amend Partner CYAs Page"))
  }

  "Partner Contact Details Check Answers Controllers" should {
    "display amend partner check answers page" in {
      val resp =
        controller.displayPage(partnershipRegistration.nominatedPartner.get.id)(FakeRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "Amend Partner CYAs Page"
    }

    "throw IllegalStateException is requested partner is not found" in {
      intercept[IllegalStateException] {
        await(controller.displayPage("xxx")(FakeRequest()))
      }
    }

    "redirect to partners list when submitted" in {
      val resp = controller.submit()(FakeRequest())

      status(resp) mustBe SEE_OTHER
      redirectLocation(resp) mustBe Some(routes.PartnersListController.displayPage().url)
    }
  }

}
