/*
 * Copyright 2024 HM Revenue & Customs
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

package models.registration

import connectors.{RegistrationConnector, ServiceError}
import models.request.JourneyRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait Cacheable {
  def registrationConnector: RegistrationConnector

  protected def update(cache: Registration => Registration)(implicit
    hc: HeaderCarrier,
    request: JourneyRequest[_]
  ): Future[Either[ServiceError, Registration]] =
    registrationConnector.update(cache(request.registration))

}
