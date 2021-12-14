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
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, SessionRecordNotFound}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.SubscriptionsConnector
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AmendmentJourneyAction.SessionId
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.RegistrationAmendmentRepository
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendmentJourneyAction @Inject() (
  appConfig: AppConfig,
  subscriptionsConnector: SubscriptionsConnector,
  registrationAmendmentRepository: RegistrationAmendmentRepository
)(implicit val exec: ExecutionContext)
    extends ActionRefiner[AuthenticatedRequest, JourneyRequest] {

  private val logger = Logger(this.getClass)

  override protected def refine[A](
    request: AuthenticatedRequest[A]
  ): Future[Either[Result, JourneyRequest[A]]] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    request.user.identityData.internalId.filter(_.trim.nonEmpty) match {
      case Some(_) =>
        request.pptReference match {
          case Some(pptReference) =>
            request.session.get(SessionId) match {
              case Some(sessionId) =>
                registrationAmendmentRepository.get(sessionId).flatMap {
                  case Some(registration) =>
                    Future.successful(
                      Right(new JourneyRequest[A](request, registration, appConfig))
                    )
                  case _ =>
                    subscriptionsConnector.getSubscription(pptReference).flatMap { registration =>
                      registrationAmendmentRepository.put(sessionId, registration).map {
                        registration =>
                          Right(new JourneyRequest[A](request, registration, appConfig))
                      }
                    }
                }
              case _ =>
                logger.warn(
                  s"Denied attempt to access ${request.uri} since no user session present"
                )
                throw SessionRecordNotFound()
            }
          case _ =>
            logger.warn(
              s"Denied attempt to access ${request.uri} since user ppt enrolment not present"
            )
            throw InsufficientEnrolments()
        }
      case None =>
        logger.warn(s"Denied attempt to access ${request.uri} since user internal id not present")
        throw InsufficientEnrolments()
    }
  }

  def updateRegistration(
    updateFunction: Registration => Registration
  )(implicit request: AuthenticatedRequest[Any], headerCarrier: HeaderCarrier) =
    registrationAmendmentRepository.update(updateFunction).flatMap { registration =>
      subscriptionsConnector.updateSubscription(
        request.pptReference.getOrElse(throw new IllegalStateException("Missing PPT enrolment")),
        registration
      )
    }

  override protected def executionContext: ExecutionContext = exec
}

object AmendmentJourneyAction {
  val SessionId = "sessionId"
}
