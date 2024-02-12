/*
 * Copyright 2024 HM Revenue & Customs
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
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import spec.PptTestData
import controllers.{routes => commonRoutes}
import forms.partner.AddPartner
import models.registration.Registration
import views.html.partner.partner_list_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnerListControllerSpec extends ControllerSpec with DefaultAwaitTimeout with PptTestData {

  private val page = mock[partner_list_page]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new PartnerListController(journeyAction = spyJourneyAction,
                              registrationConnector = mockRegistrationConnector,
                              mcc = mcc,
                              page = page
    )

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetailsWithPartners))
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    spyJourneyAction.setReg(partnershipRegistration)
    when(page.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.raw("Partner list"))
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Partner List Controller" should {
    "show list of partners" in {
      val resp = controller.displayPage()(FakeRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "Partner list"
    }

    "redisplay list of partners page with bad request status" when {
      "no selection is made as to whether another partner should be added" in {
        val resp =
          controller.submit()(postRequestEncoded(AddPartner(None)))

        status(resp) mustBe BAD_REQUEST
        contentAsString(resp) mustBe "Partner list"
      }
    }
    "redirect to other partner type selection page" when {
      "user suggests they would like to add another partner" in {
        val form = Seq("addPartner" -> "yes")
        val resp = controller.submit()(postJsonRequestEncoded(form: _*))

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(routes.PartnerTypeController.displayNewPartner().url)
      }
    }
    "redirect to task list page" when {
      "user suggests they need not add any further partner" in {
        val form = Seq("addPartner" -> "no")
        val resp = controller.submit()(postJsonRequestEncoded(form: _*))

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(commonRoutes.TaskListController.displayPage().url)
      }
    }

    "throw IllegalStateException" when {
      "nominated partner absent from registration" in {
        spyJourneyAction.setReg(withAllPartnersRemoved(partnershipRegistration))

        intercept[IllegalStateException] {
          await(controller.displayPage()(FakeRequest()))
        }
      }
    }
  }

  private def withAllPartnersRemoved(reg: Registration): Registration =
    reg.copy(organisationDetails =
      reg.organisationDetails.copy(partnershipDetails =
        reg.organisationDetails.partnershipDetails.map(_.copy(partners = Seq.empty))
      )
    )

}
