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
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.status
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.EmailAddress
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.{
  Partner,
  PartnerContactDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnerships.email_address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnershipPartnersEmailAddressControllerSpec
    extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[email_address_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new PartnershipOtherPartnerEmailAddressController(authenticate = mockAuthAction,
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

  def registrationWithInflightOtherPartnerJourney =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(
      Some(
        Partner(contactDetails =
                  Some(PartnerContactDetails(firstName = Some("John"), lastName = Some("Smith"))),
                partnerType = Some(PartnerTypeEnum.SOLE_TRADER)
        )
      )
    )

  def registrationWithInflightOtherPartnerJourneyAndEmailAddress =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(
      Some(
        Partner(
          contactDetails = Some(
            PartnerContactDetails(firstName = Some("John"),
                                  lastName = Some("Smith"),
                                  emailAddress = Some("test@localhost")
            )
          ),
          partnerType = Some(PartnerTypeEnum.SOLE_TRADER)
        )
      )
    )

  "PartnershipOtherPartnerEmailAddressController" should {

    "return 200" when {
      "user is authorised, a registration already exists with already collected contact name and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(registrationWithInflightOtherPartnerJourney)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists with already collected contact name and email address display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(registrationWithInflightOtherPartnerJourneyAndEmailAddress)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "return 404" when {
      "user is authorised but does not have an inflight journey and display page method is invoked" in {
        authorizedUser()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe NOT_FOUND
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
          controller.submit()(postRequest(Json.toJson(EmailAddress("test@test.com"))))

        intercept[DownstreamServiceError](status(result))
      }
    }
  }

}
