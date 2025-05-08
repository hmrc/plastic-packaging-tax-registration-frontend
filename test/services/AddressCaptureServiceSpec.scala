/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import base.unit.MockAddressCaptureDetailRepository
import controllers.address.routes
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import spec.PptTestData

import scala.concurrent.ExecutionContext

class AddressCaptureServiceSpec extends AnyWordSpecLike with MockAddressCaptureDetailRepository with PptTestData {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val addressCaptureService = new AddressCaptureService(inMemoryAddressCaptureDetailRepository)

  private val addressCaptureConfig = AddressCaptureConfig(
    backLink = "/back-link",
    successLink = "/success-link",
    alfHeadingsPrefix = "alf.prefix",
    pptHeadingKey = "ppt.heading",
    entityName = Some("Entity"),
    pptHintKey = None,
    forceUkAddress = false
  )

  "Address Capture Service" should {
    "store address capture config (and permit subsequent retrieval) and return suitable redirect URL" when {
      "initiating address capture" in {
        addressCaptureService.initAddressCapture(addressCaptureConfig)(getRequest()).map { redirectUrl =>
          addressCaptureService.getAddressCaptureDetails()(getRequest()).map { addressCaptureDetail =>
            addressCaptureDetail.get.config mustBe addressCaptureConfig
            addressCaptureDetail.get.capturedAddress mustBe None

            redirectUrl mustBe routes.AddressCaptureController.initialiseAddressCapture().url
          }
        }
      }
    }
    "permit storage and subsequent retrieval of captured address" in {
      addressCaptureService.setCapturedAddress(addressDetails)(getRequest()).map { _ =>
        addressCaptureService.getCapturedAddress()(getRequest()) mustBe addressDetails
      }
    }
  }

  private def getRequest() = registrationJourneyRequest.authenticatedRequest

}
