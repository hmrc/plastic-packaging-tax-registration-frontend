/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.contact

import base.unit.ControllerSpec
import connectors.DownstreamServiceError
import controllers.{routes => pptRoutes}
import forms.contact.{Address, ConfirmAddress}
import forms.organisation.OrgType.{CHARITABLE_INCORPORATED_ORGANISATION, PARTNERSHIP, REGISTERED_SOCIETY, SOLE_TRADER, UK_COMPANY}
import models.genericregistration.IncorporationAddressDetails
import models.registration.{OrganisationDetails, PrimaryContactDetails}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.contact.confirm_address

class ContactDetailsConfirmAddressControllerSpec extends ControllerSpec {
  private val page = mock[confirm_address]
  private val mcc  = stubMessagesControllerComponents()

  private val controller =
    new ContactDetailsConfirmAddressController(spyJourneyAction,
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
      OrganisationDetails(businessRegisteredAddress = Some(addressConversionUtils.toPptAddress(testCompanyAddress)),
                          organisationType = Some(UK_COMPANY)
      )
    )
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[ConfirmAddress]], any[Address], any())(any(), any())).thenReturn(
      HtmlFormat.empty
    )
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Contact Details Confirm Address Controller" should {

    "redirect to update Companies House" when {
      "Incorporation address is missing country code" in {
        spyJourneyAction.setReg(
          aRegistration(
            withOrganisationDetails(
              OrganisationDetails(organisationType = Some(UK_COMPANY),
                incorporationDetails = Some(incorporationDetails
                  .copy(companyAddress = incorporationDetails.companyAddress.copy(country = None))
                )
              )
            )
          )
        )
        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(pptRoutes.UpdateCompaniesHouseController.onPageLoad().url)
      }
    }

    "return 200" when {

      "user is authorised and display page method is invoked" in {
        spyJourneyAction.setReg(
          aRegistration(
            withOrganisationDetails(
              OrganisationDetails(organisationType = Some(UK_COMPANY),
                                  incorporationDetails = Some(incorporationDetails)
              )
            )
          )
        )
        mockRegistrationUpdate()
        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }
    }

    "show page for group membership" in {
      spyJourneyAction.setReg(
        aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(UK_COMPANY),
              incorporationDetails = Some(incorporationDetails)
            )
          ),
          withGroupDetail(Some(groupDetailsWithMembers))
        )
      )
      mockRegistrationUpdate()
      await(controller.displayPage()(FakeRequest()))

      val captor = ArgumentCaptor.forClass(classOf[Boolean])
      verify(page).apply(any(), any(), captor.capture())(any(), any())
      captor.getValue mustBe true
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
        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "display page method is invoked for partnership" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(PARTNERSHIP),
                                partnershipDetails = Some(generalPartnershipDetails)
            )
          ),
          withRegisteredBusinessAddress(testBusinessAddress)
        )

        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(FakeRequest())

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

        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(FakeRequest())

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

        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()

        val result = controller.displayPage()(FakeRequest())

        status(result) mustBe OK
      }

      "throw IllegalStateException when business registered address is missing" in {
        val registration = aRegistration(
          withOrganisationDetails(
            OrganisationDetails(organisationType = Some(CHARITABLE_INCORPORATED_ORGANISATION))
          )
        )

        spyJourneyAction.setReg(registration)
        mockRegistrationUpdate()

        intercept[IllegalStateException](status(controller.displayPage()(FakeRequest())))
      }
    }

    def primaryContactAddressPopulatedSameAs(expected: IncorporationAddressDetails): Unit =  {
      modifiedRegistration.primaryContactDetails.address.get.addressLine1 mustBe expected.premises.get
      modifiedRegistration.primaryContactDetails.address.get.addressLine2 mustBe expected.address_line_1
      modifiedRegistration.primaryContactDetails.address.get.addressLine3 mustBe expected.address_line_2
      modifiedRegistration.primaryContactDetails.address.get.townOrCity mustBe expected.locality.get
      modifiedRegistration.primaryContactDetails.address.get.maybePostcode mustBe expected.postal_code
      modifiedRegistration.primaryContactDetails.address.get.countryCode mustBe "GB"
      modifiedRegistration.primaryContactDetails.useRegisteredAddress mustBe Some(true)
    }

    def businessRegisteredAddressPopulatedSameAs(expected: IncorporationAddressDetails): Unit = {
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.addressLine1 mustBe expected.premises.get
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.addressLine2 mustBe expected.address_line_1
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.addressLine3 mustBe expected.address_line_2
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.townOrCity mustBe expected.locality.get
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.maybePostcode mustBe expected.postal_code
      modifiedRegistration.organisationDetails.businessRegisteredAddress.get.countryCode mustBe "GB"
    }

      "return 303 (OK) for " when {
        "user accepts the registered address" in {

          spyJourneyAction.setReg(registrationWithoutPrimaryContactAddress)
          mockRegistrationUpdate()

          val correctForm = Seq("useRegisteredAddress" -> "yes")
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER

          primaryContactAddressPopulatedSameAs(testCompanyAddress)
          businessRegisteredAddressPopulatedSameAs(testCompanyAddress)


              redirectLocation(result) mustBe Some(
                routes.ContactDetailsCheckAnswersController.displayPage().url
              )
        }

        "user does not accept the registered address" in {

          spyJourneyAction.setReg(registrationWithoutPrimaryContactAddress)
          mockRegistrationUpdate()

          val correctForm = Seq("useRegisteredAddress" -> "no")
          val result      = controller.submit()(postJsonRequestEncoded(correctForm: _*))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.primaryContactDetails.address mustBe None
          modifiedRegistration.primaryContactDetails.useRegisteredAddress mustBe Some(false)

          businessRegisteredAddressPopulatedSameAs(testCompanyAddress)


              redirectLocation(result) mustBe Some(
                routes.ContactDetailsAddressController.displayPage.url
              )

      }

      "return 400 (BAD_REQUEST)"  when {
        "user does not enter mandatory fields" in {

          spyJourneyAction.setReg(registrationWithoutPrimaryContactAddress)
          val result =
            controller.submit()(postRequestEncoded(JsObject.empty))

          status(result) mustBe BAD_REQUEST
        }
      }

      "return an error"  when {

        "user display page and get uk company details fails" in {
          val registration = aRegistration(
            withOrganisationDetails(OrganisationDetails(organisationType = Some(UK_COMPANY)))
          )

          spyJourneyAction.setReg(registration)

          intercept[IllegalStateException](status(controller.displayPage()(FakeRequest())))
        }

        "user submits form and the registration update fails" in {

          spyJourneyAction.setReg(registrationWithoutPrimaryContactAddress)
          mockRegistrationUpdateFailure()

          val correctForm = Seq("useRegisteredAddress" -> "yes")

          intercept[DownstreamServiceError](status(
            controller.submit()(postJsonRequestEncoded(correctForm: _*))
          ))
        }

        "user submits form and a registration update runtime exception occurs" in {

          spyJourneyAction.setReg(registrationWithoutPrimaryContactAddress)
          mockRegistrationException()

          val correctForm = Seq("useRegisteredAddress" -> "yes")

          intercept[RuntimeException](status(
            controller.submit()(postJsonRequestEncoded(correctForm: _*))
          ))
        }

      }
    }
  }
}
