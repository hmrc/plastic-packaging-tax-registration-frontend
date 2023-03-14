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

package controllers.actions.getRegistration

import connectors.SubscriptionsConnector
import models.request.AuthenticatedRequest.PPTEnrolledRequest
import models.request.JourneyRequest
import play.api.mvc.{ActionRefiner, Result}
import repositories.RegistrationAmendmentRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


//todo this is a transformer not a refiner
class GetRegistrationForAmendmentAction @Inject()(
  subscriptionsConnector: SubscriptionsConnector,
  registrationAmendmentRepository: RegistrationAmendmentRepository
)(implicit val executionContext: ExecutionContext) extends ActionRefiner[PPTEnrolledRequest, JourneyRequest] {

  override protected def refine[A](
    request: PPTEnrolledRequest[A]
  ): Future[Either[Result, JourneyRequest[A]]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      registrationAmendmentRepository.get(request.cacheId).flatMap {
        case Some(registration) =>
          Future.successful(
            Right(JourneyRequest(request, registration))
          )
        case _ =>
          subscriptionsConnector.getSubscription(request.pptReference).flatMap { registration =>
            registrationAmendmentRepository.put(request.cacheId, registration).map {
              registration =>
                Right(JourneyRequest(request, registration))
            }
          }
      }
  }

}
