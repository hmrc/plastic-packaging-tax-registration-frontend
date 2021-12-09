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
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.SubscriptionsConnector
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendmentJourneyAction @Inject() (
  appConfig: AppConfig,
  subscriptionsConnector: SubscriptionsConnector
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
            subscriptionsConnector.getSubscription(pptReference).map { registration =>
              Right(new JourneyRequest[A](request, registration, appConfig))
            }
          case _ => throw InsufficientEnrolments()
        }
      case None =>
        logger.warn(s"Enrolment not present, throwing")
        throw InsufficientEnrolments()
    }
  }

  override protected def executionContext: ExecutionContext = exec

}
