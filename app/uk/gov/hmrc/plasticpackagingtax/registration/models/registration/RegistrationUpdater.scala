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

package uk.gov.hmrc.plasticpackagingtax.registration.models.registration

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.RegistrationAmendmentRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait RegistrationUpdater {

  def updateRegistration(
    registrationUpdater: Registration => Registration
  )(implicit request: JourneyRequest[_], hc: HeaderCarrier): Future[Registration]

}

@Singleton
class AmendRegistrationUpdateService @Inject() (
  registrationAmendmentRepository: RegistrationAmendmentRepository
) extends RegistrationUpdater {

  override def updateRegistration(
    registrationUpdater: Registration => Registration
  )(implicit request: JourneyRequest[_], hc: HeaderCarrier): Future[Registration] =
    registrationAmendmentRepository.update(registrationUpdater(_))

}

@Singleton
class NewRegistrationUpdateService @Inject() (
  override val registrationConnector: RegistrationConnector
)(implicit ec: ExecutionContext)
    extends RegistrationUpdater with Cacheable {

  override def updateRegistration(
    registrationUpdater: Registration => Registration
  )(implicit request: JourneyRequest[_], hc: HeaderCarrier): Future[Registration] =
    update(registrationUpdater(_)).map {
      case Right(registration) => registration
      case Left(ex)            => throw ex
    }

}
