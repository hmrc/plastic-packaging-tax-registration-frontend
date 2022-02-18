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

package uk.gov.hmrc.plasticpackagingtax.registration.services

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.address.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.contact.Address
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.UserDataRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class AddressCaptureDetail(
  config: AddressCaptureConfig,
  capturedAddress: Option[Address] = None
)

object AddressCaptureDetail {
  implicit val format: OFormat[AddressCaptureDetail] = Json.format[AddressCaptureDetail]
}

case class AddressCaptureConfig(
  backLink: String,
  successLink: String,
  alfHeadingsPrefix: String,
  pptHeadingKey: String,
  entityName: Option[String] = None,
  pptHintKey: Option[String] = None,
  forceUkAddress: Boolean = false
)

object AddressCaptureConfig {
  implicit val format: OFormat[AddressCaptureConfig] = Json.format[AddressCaptureConfig]
}

@Singleton
class AddressCaptureService @Inject() (userDataRepository: UserDataRepository)(implicit
  ec: ExecutionContext
) {

  private val repoKey = "addressCaptureDetails"

  def initAddressCapture(
    config: AddressCaptureConfig
  )(implicit request: AuthenticatedRequest[Any]): Future[Call] =
    userDataRepository.putData(repoKey, AddressCaptureDetail(config)).map(
      _ => routes.AddressCaptureController.initialiseAddressCapture()
    )

  def getAddressCaptureDetails()(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Option[AddressCaptureDetail]] =
    userDataRepository.getData[AddressCaptureDetail](repoKey)

  def getCapturedAddress()(implicit request: AuthenticatedRequest[Any]): Future[Option[Address]] =
    getAddressCaptureDetails().map { addressCapture =>
      addressCapture.flatMap(_.capturedAddress)
    }

  def setCapturedAddress(capturedAddress: Address)(implicit request: AuthenticatedRequest[Any]) =
    userDataRepository.updateData[AddressCaptureDetail](
      repoKey,
      addressCaptureDetails => addressCaptureDetails.copy(capturedAddress = Some(capturedAddress))
    )

}
