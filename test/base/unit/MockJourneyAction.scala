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

import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyAction

import scala.concurrent.{ExecutionContext, Future}

trait MockJourneyAction
    extends MockRegistrationConnector with BeforeAndAfterEach with MockitoSugar {
  self: MockitoSugar with Suite =>
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val mockJourneyAction: JourneyAction = new JourneyAction(mockRegistrationConnector)(
    ExecutionContext.global
  )

  override protected def beforeEach() {
    super.beforeEach()
    given(mockRegistrationConnector.find(any())(any())).willReturn(
      Future.successful(Right(Option(Registration("001"))))
    )
  }

}
