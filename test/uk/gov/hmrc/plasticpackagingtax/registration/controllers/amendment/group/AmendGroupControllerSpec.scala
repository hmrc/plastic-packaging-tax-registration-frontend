/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group

import base.PptTestData.newUser
import base.unit.{ControllerSpec, MockAmendmentJourneyAction}
import play.api.mvc.{AnyContent, BodyParser, Request, Result}
import play.api.test.Helpers
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{AmendmentJourneyAction, AuthenticatedRequest, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionCreateOrUpdateResponse

import scala.concurrent.{ExecutionContext, Future}

trait AmendGroupControllerSpec extends ControllerSpec with MockAmendmentJourneyAction {

  val registration: Registration = aRegistration()

  object FakeAuthNoEnrolmentCheckAction extends AuthNoEnrolmentCheckAction {
    override def parser: BodyParser[AnyContent] = Helpers.stubControllerComponents().parsers.defaultBodyParser

    override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
      block(new AuthenticatedRequest(request, newUser()))

    override protected def executionContext: ExecutionContext = ExecutionContext.global
  }

  object FakeAmendmentJourneyAction extends AmendmentJourneyAction {
    override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, JourneyRequest[A]]] = {
      val jr = JourneyRequest(
        authenticatedRequest = request,
        registration = registration,
        appConfig = appConfig
      )

      Future.successful(Right(jr))
    }

    override protected def executionContext: ExecutionContext = ExecutionContext.global

    override def updateLocalRegistration(updateFunction: Registration => Registration)(implicit request: AuthenticatedRequest[Any]): Future[Registration] = ???

    override def updateRegistration(updateFunction: Registration => Registration)(implicit request: AuthenticatedRequest[Any], headerCarrier: HeaderCarrier): Future[SubscriptionCreateOrUpdateResponse] = ???
  }
}
