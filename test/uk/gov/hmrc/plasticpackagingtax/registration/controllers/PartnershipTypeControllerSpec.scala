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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.PartnershipTypeEnum.{
  GENERAL_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.PartnershipDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partnership_type
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.scalatest.Inspectors.forAll
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError

class PartnershipTypeControllerSpec extends ControllerSpec {
  private val page = mock[partnership_type]
  private val mcc  = stubMessagesControllerComponents()

  val controller = new PartnershipTypeController(authenticate = mockAuthAction,
                                                 journeyAction = mockJourneyAction,
                                                 appConfig = config,
                                                 generalPartnershipGrsConnector =
                                                   mockGeneralPartnershipGrsConnector,
                                                 scottishPartnershipGrsConnector =
                                                   mockScottishPartnershipGrsConnector,
                                                 registrationConnector = mockRegistrationConnector,
                                                 mcc = mcc,
                                                 page = page
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[PartnershipType]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  "Partnership Type Controller" should {

    "successfully return partnership type selection page" when {
      "no previous partnership type in registration" in {
        val registration = aRegistration(withPartnershipDetails(None))

        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "previous partnership type in registration" in {
        val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails)))

        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "redirect to partnership GRS" when {
      "a supported partnership type was selected" in {
        val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails)))

        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
        mockCreateGeneralPartnershipGrsJourneyCreation("http://test/redirect/partnership")

        val correctForm = Seq("answer" -> GENERAL_PARTNERSHIP.toString, saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(result) mustBe Some("http://test/redirect/partnership")
      }
    }

    "redirect to registration main page" when {
      "save and come back later button is used" in {
        val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails)))

        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val correctForm =
          Seq("answer" -> GENERAL_PARTNERSHIP.toString, saveAndComeBackLaterFormAction)
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(result) mustBe Some(routes.RegistrationController.displayPage().url)
      }
    }

    "redirect to unsupported business entity type page" when {
      "an unsupported partnership type was selected" in {
        val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails)))

        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()
        mockCreateGeneralPartnershipGrsJourneyCreation("http://test/redirect/partnership")

        val correctForm =
          Seq("answer" -> SCOTTISH_LIMITED_PARTNERSHIP.toString, saveAndContinueFormAction)
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(result) mustBe Some(
          routes.OrganisationTypeNotSupportedController.onPageLoad().url
        )
      }
    }

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "update registration with general partnership type for " + formAction._1 when {
        "a supported partnership type was selected" in {

          val registration = aRegistration(withPartnershipDetails(None))

          authorizedUser()
          mockRegistrationFind(registration)
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> GENERAL_PARTNERSHIP.toString, formAction)
          await(controller.submit()(postJsonRequestEncoded(correctForm: _*)))

          val expectedRegistration = registration.copy(organisationDetails =
            registration.organisationDetails.copy(partnershipDetails =
              Some(PartnershipDetails(GENERAL_PARTNERSHIP))
            )
          )
          verify(mockRegistrationConnector).update(eqTo(expectedRegistration))(any())
        }
      }

      "update registration with scottish partnership type for " + formAction._1 when {
        "a supported partnership type was selected" in {

          val registration = aRegistration(withPartnershipDetails(None))

          authorizedUser()
          mockRegistrationFind(registration)
          mockRegistrationUpdate()
          mockCreateScottishPartnershipGrsJourneyCreation("http://test/redirect/partnership")

          val correctForm = Seq("answer" -> SCOTTISH_PARTNERSHIP.toString, formAction)
          await(controller.submit()(postJsonRequestEncoded(correctForm: _*)))

          val expectedRegistration = registration.copy(organisationDetails =
            registration.organisationDetails.copy(partnershipDetails =
              Some(PartnershipDetails(SCOTTISH_PARTNERSHIP))
            )
          )
          verify(mockRegistrationConnector).update(eqTo(expectedRegistration))(any())
        }
      }
    }

    "throw errors resulting from failed registration updates for General Partnership" in {
      val registration = aRegistration(withPartnershipDetails(None))

      authorizedUser()
      mockRegistrationFind(registration)
      mockRegistrationUpdateFailure()

      val correctForm = Seq("answer" -> GENERAL_PARTNERSHIP.toString, saveAndContinueFormAction)

      intercept[DownstreamServiceError] {
        await(controller.submit()(postJsonRequestEncoded(correctForm: _*)))
      }
    }

    "returns bad request when empty form submitted" in {
      val registration = aRegistration(withPartnershipDetails(None))

      authorizedUser()
      mockRegistrationFind(registration)
      mockRegistrationUpdateFailure()

      val incompleteForm = Seq(saveAndContinueFormAction)

      val result = controller.submit()(postJsonRequestEncoded(incompleteForm: _*))

      status(result) mustBe BAD_REQUEST
    }
  }
}
