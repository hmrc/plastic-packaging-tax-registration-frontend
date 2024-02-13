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

package controllers.actions.getRegistration

import audit.Auditor
import com.google.inject.ImplementedBy
import connectors.{RegistrationConnector, ServiceError}
import models.registration.Registration
import models.request.{AuthenticatedRequest, JourneyRequest}
import play.api.Logging
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[GetRegistrationActionImpl])
trait GetRegistrationAction extends ActionRefiner[AuthenticatedRequest, JourneyRequest]

class GetRegistrationActionImpl @Inject() (registrationConnector: RegistrationConnector, auditor: Auditor)(implicit val executionContext: ExecutionContext)
    extends GetRegistrationAction with Logging {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, JourneyRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    loadOrCreateRegistration(request.cacheId)(hc).map {
      case Right(reg)  => Right(JourneyRequest(request, reg))
      case Left(error) => throw error
    }
  }

  private def loadOrCreateRegistration[A](id: String)(implicit headerCarrier: HeaderCarrier): Future[Either[ServiceError, Registration]] =
    registrationConnector.find(id).flatMap {
      case Right(Some(reg)) => Future.successful(Right(reg))
      case Right(None) =>
        auditor.newRegistrationStarted(id)
        registrationConnector.create(Registration(id))
      case Left(error) => Future.successful(Left(error))
    }

}
