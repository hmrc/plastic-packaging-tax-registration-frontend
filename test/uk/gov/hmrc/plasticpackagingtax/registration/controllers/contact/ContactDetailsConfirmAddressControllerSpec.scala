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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

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
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.{Address, ConfirmAddress}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.{
  CHARITABLE_INCORPORATED_ORGANISATION,
  PARTNERSHIP,
  REGISTERED_SOCIETY,
  SOLE_TRADER,
  UK_COMPANY
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.genericregistration.IncorporationAddressDetails
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  OrganisationDetails,
  PrimaryContactDetails
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.confirm_address
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class ContactDetailsConfirmAddressControllerSpec extends ControllerSpec {
  private val page = mock[confirm_address]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsConfirmAddressController(authenticate = mockAuthAction,
                                               mockJourneyAction,
                                               mockRegistrationConnector,
                                               mcc = mcc,
                                               page = page
    )

  private val registrationWithoutPrimaryContactAddress = aRegistration(
    withPrimaryContactDetails(
      PrimaryContactDetails(name = Some("Jack Gatsby"),
                            jobTitle = Some("Developer"),
                            email = Some("test@test.com"),
                            phoneNumber = Some("0203 4567 890"),
                            address = None
      )
    ),
    withOrganisationDetails(
      OrganisationDetails(businessRegisteredAddress = Some(testCompanyAddress.toPptAddress),
                          organisationType = Some(UK_COMPANY)
      )
    )
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[ConfirmAddress]], any[Address])(any(), any())).thenReturn(
      HtmlFormat.empty
    )
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Contact Details Confirm Address Controller" should {

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withOrganisationDetails(
              OrganisationDetails(organisationType = Some(UK_COMPANY),
                                  incorporationDetails = Some(incorporationDetails)
              )
            )
          )
        )
        mockRegistrationUpdate()
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }
    }

    "update business address" when {

      "display page method is invoked for sole trader" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(SOLE_TRADER),
                                soleTraderDetails = Some(soleTraderDetails)
            )
          ),
          withRegisteredBusinessAddress(testBusinessAddress)
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "display page method is invoked for partnership" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails = Some(partnershipDetails)
            )
          ),
          withRegisteredBusinessAddress(testBusinessAddress)
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "display page method is invoked for uk company" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(UK_COMPANY),
                                incorporationDetails = Some(incorporationDetails)
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "display page method is invoked for registered society" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(REGISTERED_SOCIETY),
                                incorporationDetails = Some(incorporationDetails)
            )
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
      }

      "throw IllegalStateException when business registered address is missing" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(CHARITABLE_INCORPORATED_ORGANISATION))
          )
        )
        authorizedUser()
        mockRegistrationFind(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(getRequest())

        intercept[IllegalStateException](status(result))
      }
    }

    def primaryContactAddressPopulatedSameAs(expected: IncorporationAddressDetails) {
      modifiedRegistration.primaryContactDetails.address.get.addressLine1 mustBe expected.premises.get
      modifiedRegistration.primaryContactDetails.address.get.addressLine2 mustBe expected.address_line_1
      modifiedRegistration.primaryContactDetails.address.get.addressLine3 mustBe expected.address_line_2
      modifiedRegistration.primaryContactDetails.address.get.townOrCity mustBe expected.locality.get
      modifiedRegistration.primaryContactDetails.address.get.postCode mustBe expected.postal_code
      modifiedRegistration.primaryContactDetails.address.get.countryCode mustBe expected.country.get
      modifiedRegistration.primaryContactDetails.useRegisteredAddress mustBe Some(true)
    }

    def businessRegisteredAddressPopulatedSameAs(expected: IncorporationAddressDetails) {
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.addressLine1 mustBe expected.premises.get
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.addressLine2 mustBe expected.address_line_1
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.addressLine3 mustBe expected.address_line_2
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.townOrCity mustBe expected.locality.get
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.postCode mustBe expected.postal_code
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.countryCode mustBe expected.country.get
    }

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (OK) for " + formAction._1 when {
        "user accepts the registered address" in {
          authorizedUser()
          mockRegistrationFind(registrationWithoutPrimaryContactAddress)
          mockRegistrationUpdate()

          val correctForm = Seq("useRegisteredAddress" -> "yes", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER

          primaryContactAddressPopulatedSameAs(testCompanyAddress)
          businessRegisteredAddressPopulatedSameAs(testCompanyAddress)

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsCheckAnswersController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }

        "user does not accept the registered address" in {
          authorizedUser()
          mockRegistrationFind(registrationWithoutPrimaryContactAddress)
          mockRegistrationUpdate()

          val correctForm = Seq("useRegisteredAddress" -> "no", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.primaryContactDetails.address mustBe None
          modifiedRegistration.primaryContactDetails.useRegisteredAddress mustBe Some(false)

          businessRegisteredAddressPopulatedSameAs(testCompanyAddress)

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsAddressController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }
      }

      "return 400 (BAD_REQUEST) for " + formAction._1 when {
        "user does not enter mandatory fields" in {
          authorizedUser()
          mockRegistrationFind(registrationWithoutPrimaryContactAddress)
          val result =
            controller.submit()(postRequestEncoded(JsObject.empty, formAction))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return an error for " + formAction._1 when {

        "user is not authorised" in {
          unAuthorizedUser()
          val result = controller.displayPage()(getRequest())

          intercept[RuntimeException](status(result))
        }

        "user display page and get uk company details fails" in {
          val registration = aRegistration(
            withOrganisationDetails(OrganisationDetails(organisationType = Some(UK_COMPANY)))
          )
          authorizedUser()
          mockRegistrationFind(registration)

          val result = controller.displayPage()(getRequest())

          intercept[IllegalStateException](status(result))
        }

        "user submits form and the registration update fails" in {
          authorizedUser()
          mockRegistrationFind(registrationWithoutPrimaryContactAddress)
          mockRegistrationUpdateFailure()

          val correctForm = Seq("useRegisteredAddress" -> "yes", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          intercept[DownstreamServiceError](status(result))
        }

        "user submits form and a registration update runtime exception occurs" in {
          authorizedUser()
          mockRegistrationFind(registrationWithoutPrimaryContactAddress)
          mockRegistrationException()

          val correctForm = Seq("useRegisteredAddress" -> "yes", formAction)
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          intercept[RuntimeException](status(result))
        }

      }
    }
  }
}
