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

package controllers.partner

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{verify, when}
import org.mockito.Mockito.never
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.Html
import forms.partner.RemovePartner
import org.mockito.MockitoSugar.reset
import play.api.test.FakeRequest
import views.html.partner.remove_partner_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class RemovePartnerControllerSpec extends ControllerSpec {

  private val mcc      = stubMessagesControllerComponents()
  private val mockPage = mock[remove_partner_page]

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  private val soleTraderPartner = aSoleTraderPartner

  private val removePartnerController = new RemovePartnerController(journeyAction = spyJourneyAction,
                                                                    mockRegistrationConnector,
                                                                    mcc,
                                                                    mockPage
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(mockPage.apply(any[Form[RemovePartner]], any(), any())(any(), any())).thenReturn(
      Html("Remove Partner Page")
    )
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(mockRegistrationConnector, mockPage)
  }

  "Remove Partner Controller" should {

    "display page when partner found in registration" in {

      spyJourneyAction.setReg(partnershipRegistration)

      val result = removePartnerController.displayPage(aSoleTraderPartner.id)(FakeRequest())

      status(result) mustBe OK
      contentAsString(result) mustBe "Remove Partner Page"
    }

    "redirect to partner list when partner not found in registration" in {

      spyJourneyAction.setReg(partnershipRegistration)

      val result = removePartnerController.displayPage(s"${soleTraderPartner.id}xxx")(FakeRequest())

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.PartnerListController.displayPage().url)
    }

    "remove identified partner when remove action confirmed" in {

      spyJourneyAction.setReg(partnershipRegistration)
      mockRegistrationUpdate()

      await(
        removePartnerController.submit(soleTraderPartner.id)(
          postJsonRequestEncoded(("value", "yes"))
        )
      )

      modifiedRegistration.organisationDetails.partnershipDetails.get.partners.size mustBe 2
      modifiedRegistration.organisationDetails.partnershipDetails.get.partners.head mustBe
        partnershipRegistration.organisationDetails.partnershipDetails.get.partners.lift(1).get
    }

    "removing last other partner should redirect to PartnerType selection page" in {

      spyJourneyAction.setReg(partnershipRegistration)
      mockRegistrationUpdate()

      await(
        removePartnerController.submit(soleTraderPartner.id)(
          postJsonRequestEncoded(("value", "yes"))
        )
      )

      modifiedRegistration.organisationDetails.partnershipDetails.get.partners.size mustBe 2
      modifiedRegistration.organisationDetails.partnershipDetails.get.partners.head mustBe
        partnershipRegistration.organisationDetails.partnershipDetails.get.partners.lift(1).get

      spyJourneyAction.setReg(modifiedRegistration)
      mockRegistrationUpdate()
      val result = removePartnerController.submit("456")(postJsonRequestEncoded(("value", "yes")))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.PartnerTypeController.displayNewPartner().url)
    }

    "leave registration unchanged when unknown partner id specified" in {

      spyJourneyAction.setReg(partnershipRegistration)
      mockRegistrationUpdate()

      await(
        removePartnerController.submit(s"${soleTraderPartner.id}xxx")(
          postJsonRequestEncoded(("value", "yes"))
        )
      )

      verify(mockRegistrationConnector, never()).update(any())(any())
    }

    "redirect to partner list when remove confirmation is negative" in {

      spyJourneyAction.setReg(partnershipRegistration)

      val result =
        removePartnerController.submit(soleTraderPartner.id)(
          postJsonRequestEncoded(("value", "no"))
        )

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.PartnerListController.displayPage().url)
    }

    "redirect to partner list when partner removal fails" in {

      mockRegistrationUpdate()
      spyJourneyAction.setReg(partnershipRegistration)
      mockRegistrationUpdateFailure()

      val result =
        removePartnerController.submit(soleTraderPartner.id)(
          postJsonRequestEncoded(("value", "yes"))
        )

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.PartnerListController.displayPage().url)
    }

    "display validation error" when {

      "no selection made" in {

        spyJourneyAction.setReg(partnershipRegistration)

        val emptySelection = Seq()
        val result =
          removePartnerController.submit(soleTraderPartner.id)(
            postJsonRequestEncoded(emptySelection: _*)
          )

        status(result) mustBe BAD_REQUEST
      }
    }
  }

}
