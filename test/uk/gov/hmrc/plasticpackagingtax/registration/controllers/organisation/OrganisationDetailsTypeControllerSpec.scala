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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation

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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  OVERSEAS_COMPANY_UK_BRANCH,
  OrgType,
  PARTNERSHIP,
  REGISTERED_SOCIETY,
  SOLE_TRADER,
  TRUST,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{
  OrgType,
  OrganisationType,
  PartnerTypeEnum
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  NewRegistrationUpdateService,
  OrganisationDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.organisation_type
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class OrganisationDetailsTypeControllerSpec extends ControllerSpec {
  private val page = mock[organisation_type]
  private val mcc  = stubMessagesControllerComponents()

  private val mockNewRegistrationUpdater = new NewRegistrationUpdateService(
    mockRegistrationConnector
  )

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
                                          appConfig = config,
                                          partnershipGrsConnector = mockPartnershipGrsConnector,
                                          registrationUpdater = mockNewRegistrationUpdater
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[OrganisationType]], any())(any(), any())).thenReturn(HtmlFormat.empty)
  }

  override protected def afterEach(): Unit = {
    reset(page,
          mockSoleTraderGrsConnector,
          mockUkCompanyGrsConnector,
          mockRegisteredSocietyGrsConnector
    )
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

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {

        "user submits organisation type: " + UK_COMPANY in {
          mockUkCompanyCreateIncorpJourneyId("http://test/redirect/uk-company")
          assertRedirectForOrgType(UK_COMPANY, "http://test/redirect/uk-company")
        }
        "user submits organisation type: " + SOLE_TRADER in {
          mockSoleTraderCreateIncorpJourneyId("http://test/redirect/sole-trader")
          assertRedirectForOrgType(SOLE_TRADER, "http://test/redirect/sole-trader")
        }
        "user submits organisation type: " + PARTNERSHIP in {
          mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")
          assertRedirectForOrgType(PARTNERSHIP,
                                   partnerRoutes.PartnershipTypeController.displayPage().url
          )
        }

        "user submits organisation type Limited Liability Partnership in group: " + PARTNERSHIP in {
          mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")
          authorizedUser()
          mockRegistrationFind(aRegistration(withRegistrationType(Some(RegType.GROUP))))
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> PARTNERSHIP.toString, formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.organisationDetails.organisationType mustBe Some(PARTNERSHIP)
          modifiedRegistration.organisationDetails.partnershipDetails.get.partnershipType mustBe PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some("http://test/redirect/partnership")
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }

        "user submits organisation type Limited Liability Partnership in group with partnership details present: " + PARTNERSHIP in {
          mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")
          authorizedUser()
          mockRegistrationFind(
            aRegistration(withRegistrationType(Some(RegType.GROUP)),
                          withPartnershipDetails(
                            Some(
                              partnershipDetailsWithBusinessAddress(partnerTypeEnum =
                                PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP
                              )
                            )
                          )
            )
          )
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> PARTNERSHIP.toString, formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.organisationDetails.organisationType mustBe Some(PARTNERSHIP)
          modifiedRegistration.organisationDetails.partnershipDetails.get.partnershipType mustBe PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some("http://test/redirect/partnership")
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }

        "user submits organisation type: " + REGISTERED_SOCIETY in {
          mockRegisteredSocietyCreateIncorpJourneyId("http://test/redirect/reg-soc")
          assertRedirectForOrgType(REGISTERED_SOCIETY, "http://test/redirect/reg-soc")
        }

        "user submits organisation type: " + CHARITABLE_INCORPORATED_ORGANISATION in {
          assertRedirectForOrgType(CHARITABLE_INCORPORATED_ORGANISATION,
                                   routes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        }
        "user submits organisation type: " + OVERSEAS_COMPANY_UK_BRANCH in {
          mockUkCompanyCreateIncorpJourneyId("http://test/redirect/overseas-uk-company")
          assertRedirectForOrgType(OVERSEAS_COMPANY_UK_BRANCH,
                                   "http://test/redirect/overseas-uk-company"
          )
        }

        "user submits organisation type: " + TRUST in {
          assertRedirectForOrgType(TRUST,
                                   routes.OrganisationTypeNotSupportedController.onPageLoad().url
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
          case "SaveAndComeBackLater" =>
            redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
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

      "user submits form for sole trader" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()
        mockSoleTraderCreateIncorpJourneyIdException()

        val correctForm = Seq("answer" -> SOLE_TRADER.toString, saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        intercept[RuntimeException](
          status(result)
        ).getMessage mustBe "sole trader create journey error"
      }

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

      "user submits form for registered society" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()
        mockRegisteredSocietyCreateIncorpJourneyIdException()

        val correctForm = Seq("answer" -> REGISTERED_SOCIETY.toString, saveAndContinueFormAction)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        intercept[RuntimeException](
          status(result)
        ).getMessage mustBe "registered society create journey error"
      }
    }

    "should support isUkCompanyPrivateBeta flag" when {

      "user submits form for uk company" in {

        mockUkCompanyCreateIncorpJourneyId("/some-url")

        journeySupportedForPrivateBeta(UK_COMPANY)
      }

      "user submits form for sole trader" in {

        journeyNotSupportedPrivateBeta(SOLE_TRADER)
      }

      "user submits form for registered society" in {

        journeyNotSupportedPrivateBeta(REGISTERED_SOCIETY)
      }

      "user submits form for partnership" in {

        journeyNotSupportedPrivateBeta(PARTNERSHIP)
      }

      "user submits form for charity (CIO)" in {

        journeyNotSupportedPrivateBeta(CHARITABLE_INCORPORATED_ORGANISATION)
      }

      "user submits form for trust" in {

        journeyNotSupportedPrivateBeta(TRUST)
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
            routes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        else
          redirectLocation(result) mustBe Some(
            routes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
      }

    }
  }
}
