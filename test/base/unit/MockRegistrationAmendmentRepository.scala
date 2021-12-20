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

import builders.RegistrationBuilder
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{
  AmendmentJourneyAction,
  AuthenticatedRequest
}
import uk.gov.hmrc.plasticpackagingtax.registration.repositories.RegistrationAmendmentRepository

import scala.concurrent.{ExecutionContext, Future}

class InMemoryRegistrationAmendmentRepository()(implicit ec: ExecutionContext)
    extends RegistrationAmendmentRepository {

  private var cache = scala.collection.mutable.Map[String, Registration]()

  def reset(): Unit = cache = scala.collection.mutable.Map[String, Registration]()

  override def put(id: String, registration: Registration): Future[Registration] = {
    cache(id) = registration
    Future.successful(registration)
  }

  override def put(registration: Registration)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[Registration] = put(getSessionId(request).get, registration)

  override def get(id: String): Future[Option[Registration]] = Future.successful(cache.get(id))

  override def get()(implicit request: AuthenticatedRequest[Any]): Future[Option[Registration]] =
    Future.successful(
      request.session.get(AmendmentJourneyAction.SessionId).flatMap(id => cache.get(id))
    )

  override def update(
    updateRegistration: Registration => Registration
  )(implicit request: AuthenticatedRequest[Any]): Future[Registration] =
    get().flatMap {
      case Some(registration) => put(updateRegistration(registration))
      case _                  => throw new IllegalStateException("Missing registration in user data repository")
    }

  private def getSessionId(request: AuthenticatedRequest[Any]) =
    request.session.get(AmendmentJourneyAction.SessionId)

}

trait MockRegistrationAmendmentRepository extends RegistrationBuilder with MockitoSugar {

  protected val inMemoryRegistrationAmendmentRepository =
    new InMemoryRegistrationAmendmentRepository()(ExecutionContext.global)

}
