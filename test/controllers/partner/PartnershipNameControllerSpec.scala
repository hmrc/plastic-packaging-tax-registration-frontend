/*
 * Copyright 2025 HM Revenue & Customs
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
import org.mockito.MockitoSugar.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.BAD_REQUEST
import play.api.test.Helpers.{await, contentAsString, status}
import play.twirl.api.HtmlFormat
import forms.organisation.PartnershipName
import play.api.test.FakeRequest
import views.html.organisation.partnership_name
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnershipNameControllerSpec extends ControllerSpec {

  private val page = mock[partnership_name]
  private val mcc  = stubMessagesControllerComponents()

  private val controller = new PartnershipNameController(
    journeyAction = spyJourneyAction,
    appConfig = config,
    partnershipGrsConnector = mockPartnershipGrsConnector,
    registrationConnector = mockRegistrationConnector,
    mcc = mcc,
    page = page
  )

  private val partnershipRegistration = aRegistration(withPartnershipDetails(Some(generalPartnershipDetails)))

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    when(page.apply(any())(any(), any())).thenReturn(HtmlFormat.raw("Partnership name capture"))
    spyJourneyAction.setReg(partnershipRegistration)
    mockRegistrationUpdate()
    mockCreatePartnershipGrsJourneyCreation("/partnership-grs-journey")

    when(config.generalPartnershipJourneyUrl).thenReturn("/general-partnership-grs-journey-creation")
    when(config.scottishPartnershipJourneyUrl).thenReturn("/scottish-partnership-grs-journey-creation")
  }

  "Partnership Name Controller" should {
    "display partnership name capture page" when {
      "user is authorized" when {
        "registration does not contain partnership name" in {
          val resp = controller.displayPage()(FakeRequest())

          contentAsString(resp) mustBe "Partnership name capture"
        }
        "registration does contain partnership name" in {
          spyJourneyAction.setReg(
            aRegistration(
              withPartnershipDetails(
                Some(generalPartnershipDetails.copy(partnershipName = Some("Partners in Plastics")))
              )
            )
          )

          val resp = controller.displayPage()(FakeRequest())

          contentAsString(resp) mustBe "Partnership name capture"
        }
      }
    }

    "update partnership name" in {

      await(controller.submit()(postRequestEncoded(PartnershipName("Partners in Plastic"))))

      modifiedRegistration.organisationDetails.partnershipDetails.get.partnershipName mustBe Some("Partners in Plastic")
    }

    "redirect to general partnership grs journey" in {

      await(controller.submit()(postRequestEncoded(PartnershipName("Partners in Plastic"))))

      val (_, grsJourneyCreationUrl) = lastPartnershipGrsJourneyCreation()

      grsJourneyCreationUrl mustBe config.generalPartnershipJourneyUrl
    }

    "redirect to scottish partnership grs journey" in {

      spyJourneyAction.setReg(aRegistration(withPartnershipDetails(Some(scottishPartnershipDetails))))

      await(controller.submit()(postRequestEncoded(PartnershipName("Partners in Plastic"))))

      val (_, grsJourneyCreationUrl) = lastPartnershipGrsJourneyCreation()

      grsJourneyCreationUrl mustBe config.scottishPartnershipJourneyUrl
    }

    "reject invalid partnership names" in {
      val resp = controller.submit()(postRequestEncoded(PartnershipName("~~~")))

      status(resp) mustBe BAD_REQUEST
    }

    "throw exception" when {
      "attempting to display partnership name capture page" when {
        "partnership details absent from the registration" in {
          spyJourneyAction.setReg(
            partnershipRegistration.copy(organisationDetails =
              partnershipRegistration.organisationDetails.copy(partnershipDetails = None)
            )
          )

          intercept[IllegalStateException] {
            await(controller.displayPage()(FakeRequest()))
          }
        }
      }

      "submitting partnership name" when {
        "registration is of unexpected partnership type" in {
          spyJourneyAction.setReg(aRegistration(withPartnershipDetails(Some(llpPartnershipDetails))))

          intercept[IllegalStateException] {
            await(controller.submit()(postRequestEncoded(PartnershipName("Partners in Plastic"))))
          }
        }
      }
    }
  }

}
