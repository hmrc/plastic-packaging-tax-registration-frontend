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
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.{RegistrationConnector, ServiceError}
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest

import scala.concurrent.{ExecutionContext, Future}

trait Cacheable {
  def registrationConnector: RegistrationConnector

  protected def update(cache: Registration => Registration)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: JourneyRequest[_]
  ): Future[Either[ServiceError, Registration]] =
    registrationConnector.update(cache(request.registration)).flatMap {
      case Right(reg)  => Future.successful(Right(reg))
      case Left(error) => Future.successful(Left(error))
    }

}
