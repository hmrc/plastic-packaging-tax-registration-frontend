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
import org.mockito.Mockito.{verify, when}
import org.mockito.ArgumentMatchers.{any, refEq}
import play.api.http.Status.OK
import play.api.mvc.{AnyContent, BodyParser, Request, Result}
import play.api.test.Helpers.{contentAsString, status}
import play.api.test.Helpers
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.actions.AuthNoEnrolmentCheckAction
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.{AmendmentJourneyAction, AuthenticatedRequest, JourneyRequest}
import uk.gov.hmrc.plasticpackagingtax.registration.models.subscriptions.SubscriptionCreateOrUpdateResponse
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.manage_group_members_page

import scala.concurrent.{ExecutionContext, Future}

class ManageGroupMembersControllerSpec extends ControllerSpec with MockAmendmentJourneyAction {

  val authRequest = new AuthenticatedRequest(request, newUser())

  val view: manage_group_members_page = mock[manage_group_members_page]

  when(view.apply(any())(any(), any())).thenReturn(Html("view"))

  val sut = new ManageGroupMembersController(FakeAuthNoEnrolmentCheckAction, FakeAmendmentJourneyAction, Helpers.stubMessagesControllerComponents(), view)

  "displayPage" must {
    "return 200 with view" in {
      val result: Future[Result] = sut.displayPage()(request)

      status(result) shouldBe OK
      contentAsString(result) shouldBe "view"
      verify(view).apply(refEq(journeyRequest.registration))(refEq(journeyRequest), any())
    }
  }


  //todo these should be used by other controllers and live somewhere else
  object FakeAuthNoEnrolmentCheckAction extends AuthNoEnrolmentCheckAction {
    override def parser: BodyParser[AnyContent] = Helpers.stubControllerComponents().parsers.defaultBodyParser
    override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
      block(authRequest.asInstanceOf[AuthenticatedRequest[A]])
    override protected def executionContext: ExecutionContext = ExecutionContext.global
  }

  object FakeAmendmentJourneyAction extends AmendmentJourneyAction {
    override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, JourneyRequest[A]]] =
      Future.successful(Right(journeyRequest.asInstanceOf[JourneyRequest[A]]))

    override protected def executionContext: ExecutionContext = ExecutionContext.global
    override def updateLocalRegistration(updateFunction: Registration => Registration)(implicit request: AuthenticatedRequest[Any]): Future[Registration] = ???
    override def updateRegistration(updateFunction: Registration => Registration)(implicit request: AuthenticatedRequest[Any], headerCarrier: HeaderCarrier): Future[SubscriptionCreateOrUpdateResponse] = ???
  }
}
