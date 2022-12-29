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

package base.unit

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import forms.contact.Address
import services.{AddressCaptureConfig, AddressCaptureService}

import scala.concurrent.Future

trait AddressCaptureSpec extends MockitoSugar {

  protected val mockAddressCaptureService = mock[AddressCaptureService]

  protected val addressCaptureRedirect = Call("GET", "/address-capture")

  protected val invalidCapturedAddress =
    Address(addressLine1 = "", addressLine2 = None, addressLine3 = Some(""), townOrCity = "", maybePostcode = Some("XXX"), countryCode = "")

  protected val validCapturedAddress = Address(
    addressLine1 = "2-3 Scala Street",
    addressLine2 = Some("Soho"),
    addressLine3 = None,
    townOrCity = "London",
    maybePostcode = Some("E17 3RE"),
    countryCode = "GB"
  )

  protected def simulateSuccessfulAddressCaptureInit(config: Option[AddressCaptureConfig]) =
    config match {
      case Some(config) =>
        when(mockAddressCaptureService.initAddressCapture(any())(any())).thenAnswer(
          inv =>
            if (inv.getArgument[AddressCaptureConfig](0) == config)
              Future.successful(addressCaptureRedirect)
            else
              throw new IllegalArgumentException(s"Unexpected arguments - expecting $config, got ${inv.getArgument(0)}")
        )
      case _ =>
        when(mockAddressCaptureService.initAddressCapture(any())(any())).thenReturn(Future.successful(addressCaptureRedirect))
    }

  protected def simulateValidAddressCapture() =
    when(mockAddressCaptureService.getCapturedAddress()(any())).thenReturn(Future.successful(Some(validCapturedAddress)))

}
