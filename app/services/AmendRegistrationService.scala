/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.SubscriptionsConnector
import models.registration.{Registration, RegistrationUpdater}
import models.request.JourneyRequest
import models.subscriptions.SubscriptionCreateOrUpdateResponse
import repositories.RegistrationAmendmentRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendRegistrationService @Inject()(
                                          subscriptionsConnector: SubscriptionsConnector,
                                          registrationAmendmentRepository: RegistrationAmendmentRepository
                                        )(implicit val executionContext: ExecutionContext) {

  def updateSubscriptionWithRegistration(
                          updateFunction: Registration => Registration = identity
                        )(implicit request: JourneyRequest[_], headerCarrier: HeaderCarrier): Future[SubscriptionCreateOrUpdateResponse] =
    registrationAmendmentRepository.update(updateFunction)(request.authenticatedRequest).flatMap { registration =>
      subscriptionsConnector.updateSubscription(
        request.pptReference.getOrElse(throw new IllegalStateException("Missing PPT enrolment")),
        registration
      )
    }
}
