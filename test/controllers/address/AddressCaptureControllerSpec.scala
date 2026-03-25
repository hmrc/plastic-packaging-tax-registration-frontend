/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.address

import base.unit.{ControllerSpec, MockAddressCaptureDetailRepository}
import config.AppConfig
import connectors.addresslookup.AddressLookupFrontendConnector
import models.addresslookup.CountryCode.GB
import models.addresslookup._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.{await, contentAsString, redirectLocation, status}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.HtmlFormat
import services.{AddressCaptureConfig, AddressCaptureService, CountryService}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.AddressConversionUtils
import views.html.address.{address_page, uk_address_page}

import scala.concurrent.Future

class AddressCaptureControllerSpec extends ControllerSpec with MockAddressCaptureDetailRepository with Injecting {

  private val mcc = stubMessagesControllerComponents()

  private val addressCaptureService = new AddressCaptureService(inMemoryAddressCaptureDetailRepository)

  private val realAppConfig                                   = inject[AppConfig]
  override val addressConversionUtils: AddressConversionUtils = inject[AddressConversionUtils]

  private val mockAddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]
  private val mockInUkPage                       = mock[uk_address_page]
  private val mockAddressPage                    = mock[address_page]

  when(mockInUkPage.apply(any(), any(), any())(any(), any())).thenReturn(HtmlFormat.raw("Is UK Address?"))
  when(mockAddressPage.apply(any(), any(), any(), any(), any(), any())(any(), any())).thenReturn(
    HtmlFormat.raw("Address Capture")
  )

  private val addressCaptureController = new AddressCaptureController(
    authenticate = FakeBasicAuthAction,
    mcc = mcc,
    addressCaptureService = addressCaptureService,
    addressLookupFrontendConnector = mockAddressLookupFrontendConnector,
    appConfig = realAppConfig,
    addressInUkPage = mockInUkPage,
    addressPage = mockAddressPage,
    countryService = new CountryService(),
    addressConversionUtils = addressConversionUtils
  )

  private val addressCaptureConfig = AddressCaptureConfig(
    backLink = "/back-link",
    successLink = "/success-link",
    alfHeadingsPrefix = "alf.prefix",
    pptHeadingKey = "ppt.heading",
    entityName = Some("Entity"),
    pptHintKey = None,
    forceUkAddress = false
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    addressCaptureService.initAddressCapture(addressCaptureConfig)(registrationJourneyRequest.authenticatedRequest)

    when(mockAddressLookupFrontendConnector.initialiseJourney(any())(any(), any())).thenReturn(
      Future.successful(AddressLookupOnRamp("/alf-on-ramp"))
    )
  }

  override protected def afterEach(): Unit = {
    reset(mockAddressLookupFrontendConnector)
    super.afterEach()
  }

  "Address Capture Controller" should {

    "display the address in uk page" in {
      val resp = addressCaptureController.addressInUk()(FakeRequest())

      status(resp) shouldBe OK
      contentAsString(resp) shouldBe "Is UK Address?"
    }

    "ask the user whether the address is uk based" when {
      "the force UK flag is *not* used" in {
        val resp = addressCaptureController.initialiseAddressCapture()(FakeRequest())

        redirectLocation(resp) shouldBe Some(routes.AddressCaptureController.addressInUk().url)
      }
    }

    "redisplay the address in uk page" when {
      "user does not make a selection" in {
        val resp = addressCaptureController.submitAddressInUk()(postRequestTuplesEncoded(Seq(("ukAddress", ""))))

        status(resp) shouldBe BAD_REQUEST
        contentAsString(resp) shouldBe "Is UK Address?"
      }
    }

    "initialise and redirect to ALF" when {

      "the force UK flag is used" in {
        val request = FakeRequest()

        val forcedUkAddressCaptureConfig = addressCaptureConfig.copy(forceUkAddress = true)
        addressCaptureService.initAddressCapture(forcedUkAddressCaptureConfig)(getAuthenticatedRequest(request))

        val resp = await(addressCaptureController.initialiseAddressCapture()(request))

        redirectLocation(Future.successful(resp)) shouldBe Some("/alf-on-ramp")
        verifyAlfInitialisedAsExpected(forcedUkAddressCaptureConfig)
      }

      "the user indicates that they wish to capture a UK address" in {
        val resp =
          await(addressCaptureController.submitAddressInUk()(postRequestTuplesEncoded(Seq(("ukAddress", "yes")))))

        redirectLocation(Future.successful(resp)) shouldBe Some("/alf-on-ramp")
        verifyAlfInitialisedAsExpected(addressCaptureConfig)
      }
    }

    "obtain and store address obtained from ALF and redirect to address capture callback" when {
      "ALF address is valid" in {
        val request         = FakeRequest()
        val validAlfAddress = aValidAlfAddress()
        simulateAlfCallback(validAlfAddress)

        val resp = await(addressCaptureController.alfCallback(Some("123"))(request))

        redirectLocation(Future.successful(resp)) shouldBe Some("/success-link")

        addressCaptureService.getCapturedAddress()(getAuthenticatedRequest(request)).map { capturedAddress =>
          capturedAddress.get.addressLine1 shouldBe validAlfAddress.address.lines.head
          capturedAddress.get.addressLine2.head shouldBe validAlfAddress.address.lines(1)
          capturedAddress.get.addressLine3.head shouldBe validAlfAddress.address.lines(2)
          capturedAddress.get.maybePostcode shouldBe Some(validAlfAddress.address.postcode)
          capturedAddress.get.countryCode shouldBe validAlfAddress.address.country.map(_.code).head
        }
      }
    }

    "obtain address from ALF but return a BAD_REQUEST status and display the PPT address capture page" when {
      "ALF address is invalid" in {
        val invalidAlfAddress = anInvalidAlfAddress()
        simulateAlfCallback(invalidAlfAddress)

        val resp = addressCaptureController.alfCallback(Some("123"))(FakeRequest())

        status(resp) shouldBe BAD_REQUEST
        contentAsString(resp) shouldBe "Address Capture"
      }
      "ALF address is missing a postcode" in {
        val validAlfAddress = aValidAlfAddress()
        val invalidAlfAddress =
          validAlfAddress.copy(address = validAlfAddress.address.copy(postcode = None))
        simulateAlfCallback(invalidAlfAddress)

        val resp = addressCaptureController.alfCallback(Some("123"))(FakeRequest())

        status(resp) shouldBe BAD_REQUEST
        contentAsString(resp) shouldBe "Address Capture"
      }
    }

    "redirect to the PPT address capture page" when {
      "user indicates that they wish to capture a non-UK address" in {
        val resp = addressCaptureController.submitAddressInUk()(postRequestTuplesEncoded(Seq(("ukAddress", "no"))))

        redirectLocation(resp) shouldBe Some(routes.AddressCaptureController.captureAddress().url)
      }
    }

    "display the PPT address capture page" in {
      val resp = addressCaptureController.captureAddress()(FakeRequest())

      status(resp) shouldBe OK
      contentAsString(resp) shouldBe "Address Capture"
    }

    "store address and redirect to address capture callback" when {
      "valid address captured in PPT address page" in {
        val request = FakeRequest()
        val validAddress = List(
          ("addressLine1", "99 Edge Road"),
          ("addressLine2", "Notting Hill"),
          ("townOrCity", "London"),
          ("postCode", "NW1 1AA"),
          ("countryCode", GB)
        )

        val resp =
          await(addressCaptureController.submitAddress()(postRequestTuplesEncoded(validAddress)))

        redirectLocation(Future.successful(resp)) shouldBe Some("/success-link")

        addressCaptureService.getCapturedAddress()(getAuthenticatedRequest(request)).map { capturedAddress =>
          capturedAddress.get.addressLine1 shouldBe validAddress.head._2
          capturedAddress.get.addressLine2.head shouldBe validAddress(1)._2
          capturedAddress.get.townOrCity shouldBe validAddress(2)._2
          capturedAddress.get.maybePostcode shouldBe Some(validAddress(3)._2)
          capturedAddress.get.countryCode shouldBe validAddress(4)._2
        }
      }
    }

    "redisplay PPT address page" when {
      "invalid address captured" in {
        val invalidAddress = List(
          ("addressLine1", "This is an address line which is greater than 35 characters in length"),
          ("addressLine2", "Notting Hill"),
          ("townOrCity", "London"),
          ("postCode", "NW1 1AA"),
          ("countryCode", GB)
        )

        val resp =
          addressCaptureController.submitAddress()(postRequestTuplesEncoded(invalidAddress))

        status(resp) shouldBe BAD_REQUEST
        contentAsString(resp) shouldBe "Address Capture"
      }
    }
  }

  private def simulateAlfCallback(addressLookupConfirmation: AddressLookupConfirmation) =
    when(mockAddressLookupFrontendConnector.getAddress(ArgumentMatchers.eq("123"))(any(), any())).thenReturn(
      Future.successful(addressLookupConfirmation)
    )

  private def aValidAlfAddress() =
    AddressLookupConfirmation(
      auditRef = "alf-123",
      id = Some("123"),
      address = AddressLookupAddress(
        lines = List("100 Old Bog Lane", "Shoreditch", "London"),
        postcode = Some("EC1 1AA"),
        country = Some(AddressLookupCountry(GB, "United Kingdom"))
      )
    )

  private def anInvalidAlfAddress() =
    AddressLookupConfirmation(
      auditRef = "alf-123",
      id = Some("123"),
      address = AddressLookupAddress(
        lines = List("This is an address line which is greater than 35 characters in length", "Shoreditch", "London"),
        postcode = Some("EC1 1AA"),
        country = Some(AddressLookupCountry(GB, "United Kingdom"))
      )
    )

  private def verifyAlfInitialisedAsExpected(addressCaptureConfig: AddressCaptureConfig) = {
    val alfInitConfigCaptor: ArgumentCaptor[AddressLookupConfigV2] =
      ArgumentCaptor.forClass(classOf[AddressLookupConfigV2])

    verify(mockAddressLookupFrontendConnector).initialiseJourney(alfInitConfigCaptor.capture())(any(), any())

    val alfConfig = alfInitConfigCaptor.getValue
    alfConfig.options.ukMode shouldBe true
    alfConfig.options.continueUrl shouldBe realAppConfig.selfUrl(routes.AddressCaptureController.alfCallback(None))
    alfConfig.labels.en.lookupPageLabels.heading shouldBe s"${addressCaptureConfig.alfHeadingsPrefix}.lookup.heading"
  }

}
