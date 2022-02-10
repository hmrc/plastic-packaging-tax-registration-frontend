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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.Helpers.{redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{routes => organisationRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.RegType
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{CHARITABLE_INCORPORATED_ORGANISATION, OVERSEAS_COMPANY_NO_UK_BRANCH, OVERSEAS_COMPANY_UK_BRANCH, OrgType, PARTNERSHIP, UK_COMPANY}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.{OrgType, OrganisationType}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.{GroupMember, OrganisationDetails}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{GroupDetail, NewRegistrationUpdateService, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_type
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.util

class OrganisationDetailsTypeControllerSpec extends ControllerSpec {
  private val page = mock[organisation_type]
  private val mcc  = stubMessagesControllerComponents()

  private val mockNewRegistrationUpdater = new NewRegistrationUpdateService(
    mockRegistrationConnector
  )

  private val controller =
    new OrganisationDetailsTypeController(authenticate = mockAuthAction,
                                          journeyAction = mockJourneyAction,
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
    when(page.apply(any[Form[OrganisationType]], any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.empty
    )
  }

  override protected def afterEach(): Unit = {
    reset(page, mockUkCompanyGrsConnector)
    super.afterEach()
  }

  "Confirm Organisation Type Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" when {
        "adding new group member" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()
          val result = controller.displayPageNewMember()(getRequest())

          status(result) mustBe OK
        }
        "amending existing group member" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()
          val result = controller.displayPageAmendMember(groupMember.id)(getRequest())

          status(result) mustBe OK
        }
      }

      "user is authorised and display page method for next group member" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withGroupDetail(
              Some(
                GroupDetail(membersUnderGroupControl = Some(true),
                            members = Seq(groupMember),
                            currentMemberOrganisationType = Some(UK_COMPANY)
                )
              )
            )
          )
        )
        mockRegistrationUpdate()
        val result = controller.displayPageNewMember()(getRequest())

        status(result) mustBe OK
      }

    }

    forAll(Seq(saveAndContinueFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {

        "user submits organisation type: " + UK_COMPANY in {
          mockUkCompanyCreateIncorpJourneyId("http://test/redirect/uk-company")
          assertRedirectForOrgType(UK_COMPANY, "http://test/redirect/uk-company")
        }
        "group user submits organisation type: " + PARTNERSHIP in {
          val registration: Registration =
            aRegistration(withRegistrationType(Some(RegType.GROUP)))
          authorizedUser()
          mockRegistrationFind(registration)
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> PARTNERSHIP.toString, formAction)
          val result      = controller.submitNewMember()(postJsonRequestEncoded(correctForm: _*))
          mockCreatePartnershipGrsJourneyCreation("http://test/redirect/partnership")
          redirectLocation(result) mustBe Some("http://test/redirect/partnership")

          val registrationCaptor: ArgumentCaptor[Registration] =
            ArgumentCaptor.forClass(classOf[Registration])
          verify(mockRegistrationConnector, times(2)).update(registrationCaptor.capture())(any())

          val values: util.List[Registration] = registrationCaptor.getAllValues
          values.get(1).organisationDetails.organisationType mustBe Some(OrgType.UK_COMPANY)
        }

        "user submits organisation type: " + PARTNERSHIP in {
          val registration: Registration =
            aRegistration(withRegistrationType(Some(RegType.SINGLE_ENTITY)))
          authorizedUser()
          mockRegistrationFind(registration)
          mockRegistrationUpdate()

          val correctForm = Seq("answer" -> PARTNERSHIP.toString, formAction)
          val result      = controller.submitNewMember()(postJsonRequestEncoded(correctForm: _*))
          redirectLocation(result) mustBe Some(
            partnerRoutes.PartnershipTypeController.displayPage().url
          )
        }

        "user submits organisation type: " + CHARITABLE_INCORPORATED_ORGANISATION in {
          assertRedirectForOrgType(
            CHARITABLE_INCORPORATED_ORGANISATION,
            organisationRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        }
        "user submits organisation type: " + OVERSEAS_COMPANY_UK_BRANCH in {
          mockUkCompanyCreateIncorpJourneyId("http://test/redirect/overseas-uk-company")
          assertRedirectForOrgType(OVERSEAS_COMPANY_UK_BRANCH,
                                   "http://test/redirect/overseas-uk-company"
          )
        }
        "user submits organisation type: " + OVERSEAS_COMPANY_NO_UK_BRANCH in {
          assertRedirectForOrgType(
            OVERSEAS_COMPANY_NO_UK_BRANCH,
            organisationRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        }
      }

      def assertRedirectForOrgType(orgType: OrgType, redirectUrl: String): Unit = {
        val groupDetails = GroupDetail(membersUnderGroupControl = Some(true),
                                       members = Seq(
                                         GroupMember(customerIdentification1 = "",
                                                     customerIdentification2 = None,
                                                     organisationDetails = Some(
                                                       OrganisationDetails(orgType.toString,
                                                                           "",
                                                                           Some("")
                                                       )
                                                     ),
                                                     addressDetails = addressDetails
                                         )
                                       )
        )
        authorizedUser()
        mockRegistrationFind(aRegistration(withGroupDetail(groupDetail = Some(groupDetails))))
        mockRegistrationUpdate()

        val correctForm = Seq("answer" -> orgType.toString, formAction)
        val result      = controller.submitNewMember()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        modifiedRegistration.groupDetail.get.members.head.organisationDetails.get.organisationType mustBe orgType.toString

        redirectLocation(result) mustBe Some(redirectUrl)
      }

      "return 400 (BAD_REQUEST) for " + formAction._1 when {
        "user does not enter mandatory fields" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          val result =
            controller.submitNewMember()(postRequestEncoded(JsObject.empty, formAction))

          status(result) mustBe BAD_REQUEST
        }

        "user enters invalid data" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          val incorrectForm = Seq("answer" -> "maybe", formAction)
          val result        = controller.submitNewMember()(postJsonRequestEncoded(incorrectForm: _*))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return an error for " + formAction._1 when {

        "user is not authorised" in {
          unAuthorizedUser()
          val result = controller.displayPageNewMember()(getRequest())

          intercept[RuntimeException](status(result))
        }

        "user submits form and the registration update fails" in {
          authorizedUser()
          mockRegistrationFind(aRegistration(withGroupDetail(groupDetail = Some(groupDetails))))
          mockRegistrationUpdateFailure()

          val correctForm = Seq("answer" -> UK_COMPANY.toString, formAction)
          val result      = controller.submitNewMember()(postJsonRequestEncoded(correctForm: _*))

          intercept[DownstreamServiceError](status(result))
        }

        "user submits form and a registration update runtime exception occurs" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationException()

          val correctForm = Seq("answer" -> UK_COMPANY.toString, formAction)
          val result      = controller.submitNewMember()(postJsonRequestEncoded(correctForm: _*))

          intercept[RuntimeException](status(result))
        }
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

        journeyNotSupportedPrivateBeta(OVERSEAS_COMPANY_UK_BRANCH)
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
        mockRegistrationFind(aRegistration(withGroupDetail(groupDetail = Some(groupDetails))))
        mockRegistrationUpdate()

        val correctForm = Seq("answer" -> orgType.toString, saveAndContinueFormAction)
        val result      = controller.submitNewMember()(postJsonRequestEncoded(correctForm: _*))

        status(result) mustBe SEE_OTHER
        if (supported)
          redirectLocation(result) must not be Some(
            organisationRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
        else
          redirectLocation(result) mustBe Some(
            organisationRoutes.OrganisationTypeNotSupportedController.onPageLoad().url
          )
      }

    }
  }
}
