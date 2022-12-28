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

package controllers.partner

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.BAD_REQUEST
import play.api.test.Helpers.{await, contentAsString, status}
import play.twirl.api.HtmlFormat
import controllers.actions.SaveAndContinue
import forms.organisation.PartnershipName
import views.html.organisation.partnership_name
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnershipNameControllerSpec extends ControllerSpec {

  private val page = mock[partnership_name]
  private val mcc  = stubMessagesControllerComponents()

  private val controller = new PartnershipNameController(
    authenticate = mockAuthAction,
    journeyAction = mockJourneyAction,
    appConfig = appConfig,
    partnershipGrsConnector = mockPartnershipGrsConnector,
    registrationConnector = mockRegistrationConnector,
    mcc = mcc,
    page = page
  )

  private val partnershipRegistration = aRegistration(
    withPartnershipDetails(Some(generalPartnershipDetails))
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    authorizedUser()
    when(page.apply(any())(any(), any())).thenReturn(HtmlFormat.raw("Partnership name capture"))
    mockRegistrationFind(partnershipRegistration)
    mockRegistrationUpdate()
    mockCreatePartnershipGrsJourneyCreation("/partnership-grs-journey")

    when(appConfig.generalPartnershipJourneyUrl).thenReturn(
      "/general-partnership-grs-journey-creation"
    )
    when(appConfig.scottishPartnershipJourneyUrl).thenReturn(
      "/scottish-partnership-grs-journey-creation"
    )
  }

  "Partnership Name Controller" should {
    "display partnership name capture page" when {
      "user is authorized" when {
        "registration does not contain partnership name" in {
          val resp = controller.displayPage()(getRequest())

          contentAsString(resp) mustBe "Partnership name capture"
        }
        "registration does contain partnership name" in {
          mockRegistrationFind(
            aRegistration(
              withPartnershipDetails(
                Some(generalPartnershipDetails.copy(partnershipName = Some("Partners in Plastics")))
              )
            )
          )

          val resp = controller.displayPage()(getRequest())

          contentAsString(resp) mustBe "Partnership name capture"
        }
      }
    }

    "update partnership name" in {
      authorizedUser()

      await(
        controller.submit()(
          postRequestEncoded(PartnershipName("Partners in Plastic"), (SaveAndContinue.toString, ""))
        )
      )

      modifiedRegistration.organisationDetails.partnershipDetails.get.partnershipName mustBe Some(
        "Partners in Plastic"
      )
    }

    "redirect to general partnership grs journey" in {
      authorizedUser()

      await(
        controller.submit()(
          postRequestEncoded(PartnershipName("Partners in Plastic"), (SaveAndContinue.toString, ""))
        )
      )

      val (_, grsJourneyCreationUrl) = lastPartnershipGrsJourneyCreation()

      grsJourneyCreationUrl mustBe appConfig.generalPartnershipJourneyUrl
    }

    "redirect to scottish partnership grs journey" in {
      authorizedUser()
      mockRegistrationFind(aRegistration(withPartnershipDetails(Some(scottishPartnershipDetails))))

      await(
        controller.submit()(
          postRequestEncoded(PartnershipName("Partners in Plastic"), (SaveAndContinue.toString, ""))
        )
      )

      val (_, grsJourneyCreationUrl) = lastPartnershipGrsJourneyCreation()

      grsJourneyCreationUrl mustBe appConfig.scottishPartnershipJourneyUrl
    }

    "reject invalid partnership names" in {
      val resp = controller.submit()(
        postRequestEncoded(PartnershipName("~~~"), (SaveAndContinue.toString, ""))
      )

      status(resp) mustBe BAD_REQUEST
    }

    "throw exception" when {
      "attempting to display partnership name capture page" when {
        "user not authorized" in {
          unAuthorizedUser()

          intercept[RuntimeException] {
            await(controller.displayPage()(getRequest()))
          }
        }
        "partnership details absent from the registration" in {
          mockRegistrationFind(
            partnershipRegistration.copy(organisationDetails =
              partnershipRegistration.organisationDetails.copy(partnershipDetails = None)
            )
          )

          intercept[IllegalStateException] {
            await(controller.displayPage()(getRequest()))
          }
        }
      }

      "submitting partnership name" when {
        "user not authorized" in {
          unAuthorizedUser()

          intercept[RuntimeException] {
            await(
              controller.submit()(
                postRequestEncoded(PartnershipName("Partners in Plastic"),
                                   (SaveAndContinue.toString, "")
                )
              )
            )
          }
        }
        "registration is of unexpected partnership type" in {
          mockRegistrationFind(aRegistration(withPartnershipDetails(Some(llpPartnershipDetails))))

          intercept[IllegalStateException] {
            await(
              controller.submit()(
                postRequestEncoded(PartnershipName("Partners in Plastic"),
                                   (SaveAndContinue.toString, "")
                )
              )
            )
          }
        }
      }
    }
  }

}
