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
import play.api.http.Status.OK
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partner.partner_check_answers_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerCheckAnswersControllerSpec
    extends ControllerSpec with DefaultAwaitTimeout with PptTestData {

  private val page = mock[partner_check_answers_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new PartnerCheckAnswersController(authenticate = mockAuthAction,
                                      journeyAction = mockJourneyAction,
                                      registrationConnector = mockRegistrationConnector,
                                      mcc = mcc,
                                      page = page
    )

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    mockRegistrationFind(partnershipRegistration)
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.raw("Partner check answers"))
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Partner Check Answers Controller" should {
    "show new partner detail" in {
      authorizedUser()
      mockRegistrationFind(
        aRegistration(withPartnershipDetails(Some(generalPartnershipDetailsWithPartners)))
      )
      val resp = controller.displayNewPartner()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "Partner check answers"
    }

    "show nominated partner detail" in {
      val resp = controller.displayExistingPartner(
        partnershipRegistration.nominatedPartner.map(_.id).get
      )(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "Partner check answers"
    }
    "show other partner detail" in {
      val firstPartnerId = partnershipRegistration.organisationDetails.partnershipDetails.map(
        _.otherPartners.head.id
      ).getOrElse(throw new IllegalStateException("Missing partner id"))
      val resp = controller.displayExistingPartner(firstPartnerId)(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "Partner check answers"
    }
    "redirect to partner list" when {
      "confirmed" in {
        val resp = controller.continue()(getRequest())

        redirectLocation(resp) mustBe Some(routes.PartnerListController.displayPage().url)
      }
    }
    "throw IllegalStateException" when {
      "nominated partner not present" in {
        mockRegistrationFind(aRegistration())

        intercept[IllegalStateException] {
          await(
            controller.displayExistingPartner(
              partnershipRegistration.nominatedPartner.map(_.id).get
            )(getRequest())
          )
        }
      }
      "specific partner not found" in {
        intercept[IllegalStateException] {
          await(controller.displayExistingPartner("XXX")(getRequest()))
        }
      }
    }
  }
}
