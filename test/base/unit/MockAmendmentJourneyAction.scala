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

package base.unit

import base.MockAuthAction
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction

import scala.concurrent.ExecutionContext

trait MockAmendmentJourneyAction
    extends MockSubscriptionConnector with MockAuthAction with AnyWordSpecLike {

  protected implicit val ec: ExecutionContext = ExecutionContext.global

  protected val mockAmendmentJourneyAction: AmendmentJourneyAction =
    new AmendmentJourneyAction(appConfig, mockSubscriptionConnector)(ExecutionContext.global)

}
