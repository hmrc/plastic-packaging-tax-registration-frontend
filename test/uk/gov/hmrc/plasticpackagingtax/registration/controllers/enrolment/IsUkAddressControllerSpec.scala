/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.enrolment

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.forms.enrolment.IsUkAddress
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.is_uk_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class IsUkAddressControllerSpec extends ControllerSpec {

  private val page = mock[is_uk_address_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller = new IsUkAddressController(mockAuthAction, mcc, page)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[IsUkAddress]])(any(), any())).thenReturn(
      HtmlFormat.raw("Is UK Address Page")
    )
  }

  "Is UK Address Controller" should {
    "display the is uk address page" when {
      "user is authorised" in {
        authorizedUser()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "Is UK Address Page"
      }
    }
    "throw a RuntimeException" when {
      "user is not authorised" in {
        unAuthorizedUser()
        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }
    }

    "redisplay the is uk address page with a BAD REQUEST status" when {
      "no selection is made" in {
        authorizedUser()
        val result = controller.submit()(postRequestEncoded(IsUkAddress(None)))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "Is UK Address Page"
      }
    }
    // TODO: this will need to change when we build out the next enrolment page
    "redisplay the is uk address page with an OK status" when {
      "a selection is made" in {
        authorizedUser()
        val result = controller.submit()(postRequestEncoded(IsUkAddress(Some(true))))

        status(result) mustBe OK
        contentAsString(result) mustBe "Is UK Address Page"
      }
    }
  }
}
