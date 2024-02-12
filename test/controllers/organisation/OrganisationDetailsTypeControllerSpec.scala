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

package controllers.organisation

import audit.Auditor
import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import controllers.partner.{routes => partnerRoutes}
import forms.liability.RegType
import forms.organisation.OrgType.{CHARITABLE_INCORPORATED_ORGANISATION, OVERSEAS_COMPANY_UK_BRANCH, OrgType, PARTNERSHIP, REGISTERED_SOCIETY, SOLE_TRADER, TRUST, UK_COMPANY}
import forms.organisation.{OrgType, OrganisationType, PartnerTypeEnum}
import models.registration.{NewRegistrationUpdateService, OrganisationDetails}
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.atLeastOnce
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.organisation.organisation_type

class OrganisationDetailsTypeControllerSpec extends ControllerSpec {
  private val page = mock[organisation_type]
  private val mcc  = stubMessagesControllerComponents()
  val mockAuditor: Auditor = mock[Auditor]

  private val mockNewRegistrationUpdater = new NewRegistrationUpdateService(
    mockRegistrationConnector
  )

  private val controller =
    new OrganisationDetailsTypeController(journeyAction = spyJourneyAction,
      auditor = mockAuditor,
      registrationConnector = mockRegistrationConnector,
      mcc = mcc,
      page = page,
      ukCompanyGrsConnector = mockUkCompanyGrsConnector,
      registeredSocietyGrsConnector =
        mockRegisteredSocietyGrsConnector,
      soleTraderGrsConnector = mockSoleTraderGrsConnector,
      appConfig = config,
      partnershipGrsConnector = mockPartnershipGrsConnector,
      registrationUpdater = mockNewRegistrationUpdater,

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

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()
        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked" in {
        val registration = aRegistration(
          withOrganisationDetails(OrganisationDetails(organisationType = Some(OrgType.UK_COMPANY)))
        )

        spyJourneyAction.setReg(registration)

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "user is authorised and display page method is invoked for representative member" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()
        val result = controller.displayPageRepresentativeMember()(FakeRequest())

        status(result) mustBe OK
      }

      "user is authorised, a registration already exists and display page method is invoked for representative member" in {
        val registration = aRegistration(
          withOrganisationDetails(OrganisationDetails(organisationType = Some(OrgType.UK_COMPANY)))
        )

        spyJourneyAction.setReg(registration)

        val result = controller.displayPageRepresentativeMember()(FakeRequest())

        status(result) mustBe OK
      }
    }

      "return 303 (OK)" when {

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

          spyJourneyAction.setReg(aRegistration(withRegistrationType(Some(RegType.GROUP))))
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> PARTNERSHIP.toString)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.organisationDetails.organisationType mustBe Some(PARTNERSHIP)
          modifiedRegistration.organisationDetails.partnershipDetails.get.partnershipType mustBe PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP


          redirectLocation(result) mustBe Some("http://test/redirect/partnership")
        }

        "user submits organisation type Limited Liability Partnership in group with partnership details present: " + PARTNERSHIP in {
          mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")

          spyJourneyAction.setReg(
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

          val correctForm = Seq("answer" -> PARTNERSHIP.toString)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.organisationDetails.organisationType mustBe Some(PARTNERSHIP)
          modifiedRegistration.organisationDetails.partnershipDetails.get.partnershipType mustBe PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP

          redirectLocation(result) mustBe Some("http://test/redirect/partnership")
        }

        "user submits organisation type: " + REGISTERED_SOCIETY in {
          mockRegisteredSocietyCreateIncorpJourneyId("http://test/redirect/reg-soc")
          assertRedirectForOrgType(REGISTERED_SOCIETY, "http://test/redirect/reg-soc")
        }

        "user submits organisation type: " + CHARITABLE_INCORPORATED_ORGANISATION in {
          assertRedirectForOrgType(CHARITABLE_INCORPORATED_ORGANISATION,
                                   routes.RegisterAsOtherOrganisationController.onPageLoad().url
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
                                   routes.RegisterAsOtherOrganisationController.onPageLoad().url
          )
        }

        "user submits organisation type Limited Liability Partnership in group: " + PARTNERSHIP + " for representative member" in {
          mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")

          spyJourneyAction.setReg(aRegistration(withRegistrationType(Some(RegType.GROUP))))
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> PARTNERSHIP.toString)
          val result =
            controller.submitRepresentativeMember()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER
          modifiedRegistration.organisationDetails.organisationType mustBe Some(PARTNERSHIP)
          modifiedRegistration.organisationDetails.partnershipDetails.get.partnershipType mustBe PartnerTypeEnum.LIMITED_LIABILITY_PARTNERSHIP

          redirectLocation(result) mustBe Some("http://test/redirect/partnership")
        }
      }

      def assertRedirectForOrgType(
        orgType: OrgType,
        redirectUrl: String
      ): Unit = {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()

        val correctForm = Seq("answer" -> orgType.toString)
        val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        modifiedRegistration.organisationDetails.organisationType mustBe Some(orgType)

        verify(mockAuditor, atLeastOnce()).orgTypeSelected(any(), refEq(Some(orgType)))(any(),
                                                                                        any()
        )

        redirectLocation(result) mustBe Some(redirectUrl)
      }

      "return 400 (BAD_REQUEST)" when {
        "user does not enter mandatory fields" in {

          spyJourneyAction.setReg(aRegistration())
          val result =
            controller.submit()(postRequestEncoded(JsObject.empty))

          status(result) mustBe BAD_REQUEST
        }

        "user enters invalid data" in {

          spyJourneyAction.setReg(aRegistration())
          val incorrectForm = Seq("answer" -> "maybe")
          val result        = controller.submit()(postJsonRequestEncoded(incorrectForm: _*))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return an error" when {

        "user submits form and the registration update fails" in {

          spyJourneyAction.setReg(aRegistration())
          mockRegistrationUpdateFailure()

          val correctForm = Seq("answer" -> UK_COMPANY.toString)

          intercept[DownstreamServiceError](status(
            controller.submit()(postJsonRequestEncoded(correctForm: _*))
          ))
        }

        "user submits form and a registration update runtime exception occurs" in {

          spyJourneyAction.setReg(aRegistration())
          mockRegistrationException()

          val correctForm = Seq("answer" -> UK_COMPANY.toString)

          intercept[RuntimeException](status(
            controller.submit()(postJsonRequestEncoded(correctForm: _*))
          ))
        }
      }
    }

    "should return error when create journey" when {

      "user submits form for sole trader" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()
        mockSoleTraderCreateIncorpJourneyIdException()

        val correctForm = Seq("answer" -> SOLE_TRADER.toString)

        intercept[RuntimeException](
          status(controller.submit()(postJsonRequestEncoded(correctForm: _*)))
        ).getMessage mustBe "sole trader create journey error"
      }

      "user submits form for uk company" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()
        mockUkCompanyCreateIncorpJourneyIdException()

        val correctForm = Seq("answer" -> UK_COMPANY.toString)

        intercept[RuntimeException](
          status(controller.submit()(postJsonRequestEncoded(correctForm: _*)))
        ).getMessage mustBe "uk company create journey error"
      }

      "user submits form for registered society" in {

        spyJourneyAction.setReg(aRegistration())
        mockRegistrationUpdate()
        mockRegisteredSocietyCreateIncorpJourneyIdException()

        val correctForm = Seq("answer" -> REGISTERED_SOCIETY.toString)

        intercept[RuntimeException](
          status(controller.submit()(postJsonRequestEncoded(correctForm: _*)))
        ).getMessage mustBe "registered society create journey error"
      }
    }

}
