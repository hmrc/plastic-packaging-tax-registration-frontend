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

package uk.gov.hmrc.plasticpackagingtax.registration.models.request

import play.api.Logger
import play.api.mvc.{ActionRefiner, Result, Results}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.RegistrationConnector
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.CreateRegistrationRequest
import uk.gov.hmrc.play.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyAction @Inject() (registrationConnector: RegistrationConnector)(implicit
  val exec: ExecutionContext
) extends ActionRefiner[AuthenticatedRequest, JourneyRequest] {

  private val logger = Logger(this.getClass)

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, JourneyRequest[A]]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    // TODO up for discussion later on what to do on various errors
    request.enrolmentId.filter(_.trim.nonEmpty) match {
      case Some(id) =>
        loadOrCreateRegistration(id).map {
          case Right(reg)  => Right(new JourneyRequest[A](request, reg, Some(id)))
          case Left(error) => throw error
        }
      case None =>
        logger.warn(s"Enrolment not present, redirecting to Start")
        Future.successful(Left(Results.Redirect(routes.StartController.displayStartPage())))
    }
  }

  private def loadOrCreateRegistration[A](id: String)(implicit headerCarrier: HeaderCarrier) =
    registrationConnector.find(id).flatMap {
      case Right(reg) =>
        reg
          .map(r => Future.successful(Right(r)))
          .getOrElse(registrationConnector.create(CreateRegistrationRequest(id)))
      case Left(error) => Future.successful(Left(error))
    }

  override protected def executionContext: ExecutionContext = exec
}
