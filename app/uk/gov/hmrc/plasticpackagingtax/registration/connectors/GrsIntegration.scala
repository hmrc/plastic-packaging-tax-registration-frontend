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

package uk.gov.hmrc.plasticpackagingtax.registration.connectors

import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future

trait GrsCreateJourneyRequest
trait GrsEntityDetails

trait GrsIntegration[
  GrsCreateJourneyPayload <: GrsCreateJourneyRequest,
  GrsResponse <: GrsEntityDetails
] {
  type RedirectUrl  = String
  type GrsJourneyId = String

  // TODO: consider implementing these methods here as they are very similar for all business entity types

  def createJourney(payload: GrsCreateJourneyPayload)(implicit
    hc: HeaderCarrier
  ): Future[RedirectUrl]

  def getDetails(journeyId: GrsJourneyId)(implicit hc: HeaderCarrier): Future[GrsResponse]
}
