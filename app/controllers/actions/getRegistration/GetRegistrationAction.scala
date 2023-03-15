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

import audit.Auditor
import connectors.{RegistrationConnector, ServiceError}
import models.registration.Registration
import models.request.{AuthenticatedRequest, JourneyRequest}
import play.api.Logging
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetRegistrationAction @Inject()(
  registrationConnector: RegistrationConnector,
  auditor: Auditor,
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[AuthenticatedRequest, JourneyRequest] with Logging {

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, JourneyRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    loadOrCreateRegistration(request.cacheId)(hc, request).map {
      case Right(reg)  => Right(JourneyRequest(request, reg))
      case Left(error) => throw error
    }
  }

  private def loadOrCreateRegistration[A](
    id: String
  )(implicit headerCarrier: HeaderCarrier, request: AuthenticatedRequest[A]): Future[Either[ServiceError, Registration]] =
    registrationConnector.find(id).flatMap {
      case Right(Some(reg)) => Future.successful(Right(reg))
      case Right(None) =>
        auditor.newRegistrationStarted(id)
        registrationConnector.create(Registration(id))
      case Left(error) => Future.successful(Left(error))
    }

}
