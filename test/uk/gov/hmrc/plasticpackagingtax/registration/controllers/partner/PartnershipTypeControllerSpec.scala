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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum.{
  GENERAL_PARTNERSHIP,
  LIMITED_LIABILITY_PARTNERSHIP,
  LIMITED_PARTNERSHIP,
  SCOTTISH_LIMITED_PARTNERSHIP,
  SCOTTISH_PARTNERSHIP
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.PartnershipDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partnership_type
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class PartnershipTypeControllerSpec extends ControllerSpec {
  private val page = mock[partnership_type]
  private val mcc  = stubMessagesControllerComponents()

  val controller = new PartnershipTypeController(authenticate = mockAuthAction,
                                                 journeyAction = mockJourneyAction,
                                                 appConfig = config,
                                                 soleTraderGrsConnector =
                                                   mockSoleTraderGrsConnector,
                                                 ukCompanyGrsConnector = mockUkCompanyGrsConnector,
                                                 partnershipGrsConnector =
                                                   mockPartnershipGrsConnector,
                                                 registrationConnector = mockRegistrationConnector,
                                                 mcc = mcc,
                                                 page = page
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[PartnerType]])(any(), any())).thenReturn(HtmlFormat.empty)
    mockCreatePartnershipGrsJourneyCreation("/partnership-grs-journey")
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
        val registration = aRegistration(withPartnershipDetails(Some(generalPartnershipDetails)))

        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "redirect to partnership GRS" when {
      forAll(
        Seq((SCOTTISH_LIMITED_PARTNERSHIP, scottishPartnershipDetails),
            (LIMITED_PARTNERSHIP, limitedPartnershipDetails),
            (LIMITED_LIABILITY_PARTNERSHIP, llpPartnershipDetails)
        )
      ) { partnershipDetails =>
        s"a ${partnershipDetails._1} type was selected" in {
          val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails._2)))

          authorizedUser()
          mockRegistrationFind(registration)
          mockRegistrationUpdate()
          mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")

          val correctForm =
            Seq("answer" -> partnershipDetails._1.toString, saveAndContinueFormAction)
          val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          redirectLocation(result) mustBe Some("http://test/redirect/partnership")
        }
      }
    }

    "capture partnership name" when {
      forAll(
        Seq((GENERAL_PARTNERSHIP, generalPartnershipDetails),
            (SCOTTISH_PARTNERSHIP, scottishPartnershipDetails)
        )
      ) { partnershipDetails =>
        s"a ${partnershipDetails._1} type was selected" in {
          val registration = aRegistration(withPartnershipDetails(Some(partnershipDetails._2)))

          authorizedUser()
          mockRegistrationFind(registration)
          mockRegistrationUpdate()

          val correctForm =
            Seq("answer" -> partnershipDetails._1.toString, saveAndContinueFormAction)
          val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          redirectLocation(result) mustBe Some(routes.PartnershipNameController.displayPage().url)
        }
      }
    }

    "redirect to registration main page" when {
      "save and come back later button is used" in {
        val registration = aRegistration(withPartnershipDetails(Some(generalPartnershipDetails)))

        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val correctForm =
          Seq("answer" -> GENERAL_PARTNERSHIP.toString, saveAndComeBackLaterFormAction)
        val result = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
      }
    }

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      forAll(
        Seq(GENERAL_PARTNERSHIP,
            SCOTTISH_PARTNERSHIP,
            SCOTTISH_LIMITED_PARTNERSHIP,
            LIMITED_PARTNERSHIP,
            LIMITED_LIABILITY_PARTNERSHIP
        )
      ) { partnershipType =>
        s"update registration with $partnershipType type for " + formAction._1 in {
          val registration = aRegistration(withPartnershipDetails(None))

          authorizedUser()
          mockRegistrationFind(registration)
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> partnershipType.toString, formAction)
          await(controller.submit()(postJsonRequestEncoded(correctForm: _*)))

          val expectedRegistration = registration.copy(organisationDetails =
            registration.organisationDetails.copy(partnershipDetails =
              Some(PartnershipDetails(partnershipType))
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
