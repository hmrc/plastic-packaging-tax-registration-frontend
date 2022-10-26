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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.JobTitle
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_job_title_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerJobTitleControllerSpec extends ControllerSpec with DefaultAwaitTimeout {

  private val page = mock[partner_job_title_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new PartnerJobTitleController(authenticate = mockAuthAction,
                                  journeyAction = mockJourneyAction,
                                  registrationConnector =
                                    mockRegistrationConnector,
                                  mcc = mcc,
                                  page = page
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  private def registrationWithPartnershipDetailsAndInflightPartner =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).withInflightPartner(
      Some(aLimitedCompanyPartner())
    )

  private def registrationWithPartnershipDetailsAndNonNominatedInflightPartner =
    aRegistration(withPartnershipDetails(Some(generalPartnershipDetails))).addOtherPartner(
      aSoleTraderPartner
    ).withInflightPartner(Some(aLimitedCompanyPartner()))

  private val existingPartner =
    aLimitedCompanyPartner()

  private def registrationWithExistingPartner =
    aRegistration(
      withPartnershipDetails(Some(generalPartnershipDetails.copy(partners = Seq(existingPartner))))
    )

  "PartnerJobTitleController" should {

    "return 200" when {
      "user is authorised, a registration already exists with already collected nominated partner" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartner)

        val result = controller.displayNewPartner()(getRequest())

        status(result) mustBe OK
      }

      "displaying an existing partner to edit their job title" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)

        val result = controller.displayExistingPartner(existingPartner.id)(getRequest())

        status(result) mustBe OK
      }
    }

    "update inflight registration" when {
      "user submits a complete job title" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndNonNominatedInflightPartner)
        mockRegistrationUpdate()

        val result = controller.submitNewPartner()(
          postRequestEncoded(JobTitle("Director"), saveAndContinueFormAction)
        )

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.PartnerEmailAddressController.displayNewPartner().url
        )

        modifiedRegistration.inflightPartner.flatMap(
          _.contactDetails.flatMap(_.jobTitle)
        ) mustBe Some("Director")
      }

      "user submits an amendment to an existing partners job title" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)
        mockRegistrationUpdate()

        val result = controller.submitExistingPartner(existingPartner.id)(
          postRequest(Json.toJson(JobTitle("Company secretary")))
        )

        status(result) mustBe SEE_OTHER

        val modifiedContactDetails =
          modifiedRegistration.findPartner(existingPartner.id).flatMap(_.contactDetails)
        modifiedContactDetails.flatMap(_.jobTitle) mustBe Some("Company secretary")
      }
    }

    "return 400 (BAD_REQUEST)" when {
      "user does not enter name" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartner)

        val result =
          controller.submitNewPartner()(postRequestEncoded(JobTitle(""), saveAndContinueFormAction))

        status(result) mustBe BAD_REQUEST
      }

      "user enters a long title" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartner)

        val result = controller.submitNewPartner()(
          postRequestEncoded(JobTitle("abced" * 40), saveAndContinueFormAction)
        )

        status(result) mustBe BAD_REQUEST
      }

      "user enters non-alphabetic characters" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartner)

        val result =
          controller.submitNewPartner()(
            postRequestEncoded(JobTitle("Director 123"), saveAndContinueFormAction)
          )

        status(result) mustBe BAD_REQUEST
      }
    }

    "return an error" when {

      "user is not authorised" in {
        unAuthorizedUser()

        val result = controller.displayNewPartner()(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user tries to display an non existent partner" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)

        val result = controller.displayExistingPartner("not-an-existing-partner-id")(getRequest())

        intercept[RuntimeException](status(result))
      }

      "user submits an amendment to a non existent partner" in {
        authorizedUser()
        mockRegistrationFind(registrationWithExistingPartner)
        mockRegistrationUpdate()

        val result = controller.submitExistingPartner("not-an-existing-partners-id")(
          postRequestEncoded(JobTitle("Director"), saveAndContinueFormAction)
        )

        intercept[RuntimeException](status(result))
      }

      "user submits form and the registration update fails" in {
        authorizedUser()
        mockRegistrationFind(registrationWithPartnershipDetailsAndInflightPartner)
        mockRegistrationUpdateFailure()

        val result =
          controller.submitNewPartner()(postRequest(Json.toJson(JobTitle("Director"))))

        intercept[DownstreamServiceError](status(result))
      }
    }
  }

}
