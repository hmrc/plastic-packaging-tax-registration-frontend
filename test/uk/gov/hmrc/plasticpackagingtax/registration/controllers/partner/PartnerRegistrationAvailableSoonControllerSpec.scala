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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_registration_available_soon_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerRegistrationAvailableSoonControllerSpec extends ControllerSpec {

  private val page = mock[partner_registration_available_soon_page]

  val controller =
    new PartnerRegistrationAvailableSoonController(stubMessagesControllerComponents(),
                                                   page,
                                                   authenticate = mockAuthAction
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply()(any(), any())).thenReturn(HtmlFormat.empty)
    authorizedUser()
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "PartnerRegistrationAvailableSoon controller" should {

    "return 200 (OK)" when {

      "on page load method is invoked" in {

        val result = controller.onPageLoad()(getRequest())

        status(result) must be(OK)
      }
    }
  }
}