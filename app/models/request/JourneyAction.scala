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

package models.request

import play.api.Logger
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.http.HeaderCarrier
import audit.Auditor
import connectors.RegistrationConnector
import models.registration.Registration
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

//todo why does this live here??
class JourneyAction @Inject() (
  registrationConnector: RegistrationConnector,
  auditor: Auditor,
)(implicit val exec: ExecutionContext)
    extends ActionRefiner[AuthenticatedRequest, JourneyRequest] {

  private val logger = Logger(this.getClass)

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, JourneyRequest[A]]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    request.user.identityData.internalId.filter(_.trim.nonEmpty) match {
      case Some(id) =>
        loadOrCreateRegistration(id)(hc, request).map {
          case Right(reg)  => Right(JourneyRequest[A](request, reg, request.pptReference))
          case Left(error) => throw error
        }
      case None =>
        logger.warn(s"Denied attempt to access ${request.uri} since user internal id not present")
        throw InsufficientEnrolments()
    }
  }

  private def loadOrCreateRegistration[A](
    id: String
  )(implicit headerCarrier: HeaderCarrier, request: AuthenticatedRequest[A]) =
    registrationConnector.find(id).flatMap {
      case Right(reg) =>
        reg
          .map { r =>
            val hasPptRegistrationResumed =
              request.session.get("resumePPTRegistration").getOrElse("false")
            if (hasPptRegistrationResumed.equals("false"))
              auditor.resumePPTRegistration(id, r.organisationDetails.organisationType)
            Future.successful(Right(r))
          }
          .getOrElse {
            auditor.newRegistrationStarted(id)
            registrationConnector.create(Registration(id))
          }
      case Left(error) => Future.successful(Left(error))
    }

  override protected def executionContext: ExecutionContext = exec
}