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
import play.api.http.Status.OK
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.partner.manage_partners_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.plasticpackagingtax.registration.utils.FakeRequestCSRFSupport._

class ManagePartnersControllerSpec extends ControllerSpec with MockAmendmentJourneyAction {

  private val mcc                    = stubMessagesControllerComponents()
  private val mockManagePartnersPage = mock[manage_partners_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private val managePartnersController = new ManagePartnersController(
    authenticate = mockAuthAllowEnrolmentAction,
    amendmentJourneyAction = mockAmendmentJourneyAction,
    mcc = mcc,
    page = mockManagePartnersPage
  )

  override protected def beforeEach(): Unit =
    when(
      mockManagePartnersPage.apply(ArgumentMatchers.eq(partnershipRegistration))(any(), any())
    ).thenReturn(Html("Manage Partners Page"))

  "Manage Partners Controller" should {
    "display the manage partners page" in {
      authorisedUserWithPptSubscription()
      simulateGetSubscriptionSuccess(partnershipRegistration)

      val resp = managePartnersController.displayPage()(getRequest())
      status(resp) mustBe OK
      contentAsString(resp) mustBe "Manage Partners Page"
    }
  }

  private def getRequest(): Request[AnyContentAsEmpty.type] =
    FakeRequest("GET", "").withSession((AmendmentJourneyAction.SessionId, "123")).withCSRFToken

}
