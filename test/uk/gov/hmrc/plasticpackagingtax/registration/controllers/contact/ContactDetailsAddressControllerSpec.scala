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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.contact

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.Inspectors.forAll
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.DownstreamServiceError
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.addresslookup.AddressLookupFrontendConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.{routes => pptRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.addresslookup.{
  AddressLookupAddress,
  AddressLookupConfirmation,
  AddressLookupOnRamp,
  MissingAddressIdException
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.PrimaryContactDetails
import uk.gov.hmrc.plasticpackagingtax.registration.services.CountryService
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.contact.address_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.Future

class ContactDetailsAddressControllerSpec extends ControllerSpec {

  private val page           = mock[address_page]
  private val mcc            = stubMessagesControllerComponents()
  private val countryService = app.injector.instanceOf[CountryService]

  private val mockAddressLookupFrontendConnector: AddressLookupFrontendConnector =
    mock[AddressLookupFrontendConnector]

  private val controller =
    new ContactDetailsAddressController(authenticate = mockAuthAction,
                                        mockJourneyAction,
                                        mockRegistrationConnector,
                                        mockAddressLookupFrontendConnector,
                                        config,
                                        mcc = mcc,
                                        page = page,
                                        countryService = countryService
    )

  private val anAddress =
    Address(addressLine1 = "Address Line 1",
            addressLine2 = Some("Address Line 2"),
            townOrCity = "townOrCity",
            postCode = Some("LS3 3UJ")
    )

  private val invalidLookupConfirmation = AddressLookupConfirmation(
    "auditRef",
    Some("id"),
    AddressLookupAddress(lines = List.empty, postcode = None, country = None)
  )

  private val validLookupConfirmation = AddressLookupConfirmation("auditRef",
                                                                  Some("id"),
                                                                  AddressLookupAddress(
                                                                    lines =
                                                                      List(
                                                                        anAddress.addressLine1,
                                                                        anAddress.addressLine2.getOrElse(
                                                                          ""
                                                                        ),
                                                                        anAddress.townOrCity
                                                                      ),
                                                                    postcode = anAddress.postCode,
                                                                    country = None
                                                                  )
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(page.apply(any[Form[Address]], any(), any(), any())(any(), any())).thenReturn(
      HtmlFormat.raw("Contact Address Page")
    )
    when(mockAddressLookupFrontendConnector.initialiseJourney(any())(any(), any())).thenReturn(
      Future(AddressLookupOnRamp("/on-ramp"))
    )
  }

  override protected def afterEach(): Unit = {
    reset(page)
    super.afterEach()
  }

  "Primary Contact Details Address Controller" should {

    "display page" when {

      "user is authorised and registration exists with a primary contact address" in {
        authorizedUser()
        mockRegistrationFind(
          aRegistration(
            withPrimaryContactDetails(
              PrimaryContactDetails(address =
                Some(anAddress)
              )
            )
          )
        )
        val result = controller.displayPage()(getRequest())

        status(result) mustBe OK
        contentAsString(result) mustBe "Contact Address Page"
      }

    }

    "redirect to address lookup frontend" when {

      "display page method is invoked with no existing address" in {
        authorizedUser()

        val result = controller.displayPage()(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/on-ramp")
      }
    }

    "initialise" should {
      "redirect to address lookup frontend" in {
        authorizedUser()

        val result = controller.initialise()(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/on-ramp")
      }
    }

    "update" should {
      "throw an exception when called without an id" in {
        authorizedUser()

        val result = controller.update(None)(getRequest())

        intercept[MissingAddressIdException](status(result))
      }

      "display an error when the returned address is incompatible with PPT address" in {
        authorizedUser()

        when(
          mockAddressLookupFrontendConnector.getAddress(ArgumentMatchers.eq("invalid"))(any(),
                                                                                        any()
          )
        ).thenReturn(Future(invalidLookupConfirmation))

        val result = controller.update(Some("invalid"))(getRequest())

        status(result) mustBe BAD_REQUEST
      }

      "persist and redirect when compatible address is returned" in {
        authorizedUser()
        mockRegistrationFind(aRegistration())
        mockRegistrationUpdate()

        when(
          mockAddressLookupFrontendConnector.getAddress(ArgumentMatchers.eq("valid"))(any(), any())
        ).thenReturn(Future(validLookupConfirmation))

        val result = controller.update(Some("valid"))(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          routes.ContactDetailsCheckAnswersController.displayPage().url
        )

        modifiedRegistration.primaryContactDetails.address mustBe Some(anAddress)
      }
    }

    forAll(Seq(saveAndContinueFormAction, saveAndComeBackLaterFormAction)) { formAction =>
      "return 303 (SEE_OTHER) for " + formAction._1 when {
        "user submits or saves the contact address" in {
          authorizedUser()
          mockRegistrationFind(aRegistration())
          mockRegistrationUpdate()

          val result =
            controller.submit()(postRequestEncoded(anAddress, formAction))

          status(result) mustBe SEE_OTHER

          modifiedRegistration.primaryContactDetails.address mustBe Some(anAddress)

          formAction._1 match {
            case "SaveAndContinue" =>
              redirectLocation(result) mustBe Some(
                routes.ContactDetailsCheckAnswersController.displayPage().url
              )
            case "SaveAndComeBackLater" =>
              redirectLocation(result) mustBe Some(pptRoutes.TaskListController.displayPage().url)
          }
        }
      }

      "return 400 (BAD_REQUEST) for " + formAction._1 when {
        "user does not enter mandatory fields" in {
          authorizedUser()
          val invalidAddress =
            Address(addressLine1 = "", townOrCity = "", postCode = Some(""))
          val result =
            controller.submit()(postRequestEncoded(invalidAddress, formAction))

          status(result) mustBe BAD_REQUEST
        }

        "user enters invalid data" in {
          authorizedUser()
          val invalidAddress = Address(addressLine1 = "Address Line 1",
                                       addressLine2 = Some("Address Line ****"),
                                       townOrCity = "townOrCity",
                                       postCode = Some("LS3 3UJ")
          )
          val result = controller.submit()(postRequestEncoded(invalidAddress, formAction))

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
          mockRegistrationUpdateFailure()
          val result =
            controller.submit()(postRequestEncoded(anAddress, formAction))

          intercept[DownstreamServiceError](status(result))
        }

        "user submits form and a registration update runtime exception occurs" in {
          authorizedUser()
          mockRegistrationException()
          val result =
            controller.submit()(postRequestEncoded(anAddress, formAction))

          intercept[RuntimeException](status(result))
        }
      }
    }
  }
}
