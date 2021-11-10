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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.OrgType.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  OVERSEAS_COMPANY,
  OVERSEAS_COMPANY_NO_UK_BRANCH,
  OrgType,
  PARTNERSHIP,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{OrgType, OrganisationType}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.OrganisationDetails
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_type
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class OrganisationDetailsTypeControllerSpec extends ControllerSpec {
  private val page = mock[organisation_type]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new OrganisationDetailsTypeController(authenticate = mockAuthAction,
                                          journeyAction = mockJourneyAction,
                                          registrationConnector = mockRegistrationConnector,
                                          mcc = mcc,
                                          page = page,
                                          ukCompanyGrsConnector = mockUkCompanyGrsConnector,
                                          registeredSocietyGrsConnector =
                                            mockRegisteredSocietyGrsConnector,
                                          soleTraderGrsConnector = mockSoleTraderGrsConnector,
                                          appConfig = config
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[OrganisationType]])(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page, mockUkCompanyGrsConnector)
    super.afterEach()
  }

  "Confirm Organisation Type Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        val registration = aRegistration(
          withOrganisationDetails(OrganisationDetails(organisationType = Some(OrgType.UK_COMPANY)))
        )
        authorizedUser()
        mockRegistrationFind(registration)

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    forAll(Seq(saveAndContinueFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {

        "user submits organisation type: " + UK_COMPANY in {
          mockUkCompanyCreateIncorpJourneyId("http://test/redirect/uk-company")
          assertRedirectForOrgType(UK_COMPANY, "http://test/redirect/uk-company")
        }
        "user submits organisation type: " + PARTNERSHIP in {
          mockCreateGeneralPartnershipGrsJourneyCreation("http://test/redirect/partnership")
          assertRedirectForOrgType(PARTNERSHIP,
                                   pptRoutes.PartnershipTypeController.displayPage().url
          )
        }

        "user submits organisation type: " + CHARITABLE_INCORPORATED_ORGANISATION in {
          assertRedirectForOrgType(CHARITABLE_INCORPORATED_ORGANISATION,
                                   pptRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        }
        "user submits organisation type: " + OVERSEAS_COMPANY in {
          assertRedirectForOrgType(OVERSEAS_COMPANY,
                                   pptRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        }
        "user submits organisation type: " + OVERSEAS_COMPANY_NO_UK_BRANCH in {
          assertRedirectForOrgType(OVERSEAS_COMPANY_NO_UK_BRANCH,
                                   pptRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        }
      }

      def assertRedirectForOrgType(orgType: OrgType, redirectUrl: String): Unit = {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val correctForm = Seq("answer" -> orgType.toString, formAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        modifiedRegistration.organisationDetails.organisationType mustBe Some(orgType)

        formAction._1 match {
          case "SaveAndContinue" =>
            redirectLocation(result) mustBe Some(redirectUrl)
          case _ =>
            redirectLocation(result) mustBe Some(pptRoutes.RegistrationController.displayPage().url)
        }
      }

      "return 400 (BAD_REQUEST) for " + formAction._1 when {
        "user does not enter mandatory fields" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          val result =
            controller.submit()(postRequestEncoded(JsObject.empty, formAction))

          status(result) mustBe BAD_REQUEST
        }

        "user enters invalid data" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          val incorrectForm = Seq("answer" -> "maybe", formAction)
          val result        = controller.submit()(postJsonRequestEncoded(incorrectForm: _*))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return an error for " + formAction._1 when {

        "user is not authorised" in {
          unAuthorizedUser()
          val result = controller.displayPage()(getRequest())

          intercept[RuntimeException](status(result))
        }

        "user submits form and the registration update fails" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdateFailure()

          val correctForm = Seq("answer" -> UK_COMPANY.toString, formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          intercept[DownstreamServiceError](status(result))
        }

        "user submits form and a registration update runtime exception occurs" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationException()

          val correctForm = Seq("answer" -> UK_COMPANY.toString, formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          intercept[RuntimeException](status(result))
        }
      }
    }

    "should return error when create journey" when {

      "user submits form for uk company" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()
        mockUkCompanyCreateIncorpJourneyIdException()

        val correctForm = Seq("answer" -> UK_COMPANY.toString, saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        intercept[RuntimeException](
          status(result)
        ).getMessage mustBe "uk company create journey error"
      }
    }

    "should support isUkCompanyPrivateBeta flag" when {

      "user submits form for uk company" in {

        mockUkCompanyCreateIncorpJourneyId("/some-url")

        journeySupportedForPrivateBeta(UK_COMPANY)
      }

      "user submits form for partnership" in {

        journeyNotSupportedPrivateBeta(PARTNERSHIP)
      }

      "user submits form for overseas company" in {

        journeyNotSupportedPrivateBeta(OVERSEAS_COMPANY)
      }

      "user submits form for overseas company with no branch in uk" in {

        journeyNotSupportedPrivateBeta(OVERSEAS_COMPANY_NO_UK_BRANCH)
      }

      def journeyNotSupportedPrivateBeta(orgType: OrgType.Value) =
        journeySupportedForPrivateBeta(orgType, false)

      def journeySupportedForPrivateBeta(orgType: OrgType.Value, supported: Boolean = true) = {
        authorizedUser(features =
          Map(Features.isPreLaunch -> true, Features.isUkCompanyPrivateBeta -> true)
        )
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        val correctForm = Seq("answer" -> orgType.toString, saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        if (supported)
          redirectLocation(result) must not be Some(
            pptRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        else
          redirectLocation(result) mustBe Some(
            pptRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
      }

    }
  }
}
