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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.partnership

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.MemberName
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.member_name_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnershipPartnersContactNameControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[member_name_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new PartnershipOtherPartnerContactNameController(authenticate = mockAuthAction,
                                                     journeyAction = mockJourneyAction,
                                                     registrationConnector =
                                                       mockRegistrationConnector,
                                                     mcc = mcc,
                                                     page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  private def registrationWithPartnershipDetails =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails)))

  "PartnershipOtherPartnerEmailAddressController" should {

    "return 200" when {
      "user is authorised, a registration already exists with already collected nominated partner" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetails)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user does not enter name" in {
        authorizedUser()
        val result =
          controller.submit()(postRequestEncoded(MemberName("John", ""), saveAndContinueFormAction))

        status(result) mustBe BAD_REQUEST
      }

      "user enters a long name" in {
        authorizedUser()
        val result = controller.submit()(
          postRequestEncoded(MemberName("abced" * 40, "Smith"), saveAndContinueFormAction)
        )

        status(result) mustBe BAD_REQUEST
      }

      "user enters non-alphabetic characters" in {
        authorizedUser()
        val result =
          controller.submit()(
            postRequestEncoded(MemberName("FirstNam807980234£$", "LastName"),
                               saveAndContinueFormAction
            )
          )

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()

        val result = controller.displayPage()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationUpdateFailure()
        val result =
          controller.submit()(postRequest(Json.toJson(MemberName("John", "Smith"))))

        intercept[DownstreamServiceError](status(result))
      }
    }
  }

}
