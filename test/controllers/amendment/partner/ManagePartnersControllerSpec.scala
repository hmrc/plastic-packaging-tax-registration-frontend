/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.amendment.partner.manage_partners_page

class ManagePartnersControllerSpec extends ControllerSpec with AmendmentControllerSpec {

  private val mcc                    = stubMessagesControllerComponents()
  private val mockManagePartnersPage = mock[manage_partners_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private val managePartnersController = new ManagePartnersController(
    mcc = mcc,
    page = mockManagePartnersPage,
    journeyAction = spyJourneyAction
  )

  override protected def beforeEach(): Unit =
    when(
      mockManagePartnersPage.apply(ArgumentMatchers.eq(partnershipRegistration))(any(), any())
    ).thenReturn(Html("Manage Partners Page"))

  "Manage Partners Controller" should {
    "display the manage partners page" in {

      spyJourneyAction.setReg(partnershipRegistration)

      val resp = managePartnersController.displayPage()(FakeRequest())
      status(resp) mustBe OK
      contentAsString(resp) mustBe "Manage Partners Page"
    }
  }


}
