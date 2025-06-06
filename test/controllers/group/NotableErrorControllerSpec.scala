/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.group

import base.unit.ControllerSpec
import controllers.actions.getRegistration.GetRegistrationAction
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import controllers.group.{routes => groupRoutes}
import models.registration.GroupDetail
import models.registration.group.GroupError
import models.registration.group.GroupErrorType.{MEMBER_IN_GROUP, MEMBER_IS_ALREADY_REGISTERED}
import models.request.{AuthenticatedRequest, JourneyRequest}
import play.api.mvc.Result
import play.api.test.FakeRequest
import views.html.group.{group_member_already_registered_page, nominated_organisation_already_registered_page, organisation_already_in_group_page}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.{ExecutionContext, Future}

class NotableErrorControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val nominatedOrganisationAlreadyRegisteredPage =
    mock[nominated_organisation_already_registered_page]

  private val organisationAlreadyInGroupPage   = mock[organisation_already_in_group_page]
  private val groupMemberAlreadyRegisteredPage = mock[group_member_already_registered_page]

  object FakeGetRegAction extends GetRegistrationAction {

    override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, JourneyRequest[A]]] =
      Future.successful(Right(JourneyRequest(getAuthenticatedRequest(request), spyJourneyAction.registration.get)))

    override protected def executionContext: ExecutionContext = ec
  }

  private val controller =
    new NotableErrorController(
      authenticate = FakeBasicAuthAction,
      mcc = mcc,
      nominatedOrganisationAlreadyRegisteredPage = nominatedOrganisationAlreadyRegisteredPage,
      organisationAlreadyInGroupPage = organisationAlreadyInGroupPage,
      groupMemberAlreadyRegisteredPage = groupMemberAlreadyRegisteredPage,
      getRegistration = FakeGetRegAction
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(nominatedOrganisationAlreadyRegisteredPage.apply()(any(), any())).thenReturn(
      HtmlFormat.raw("error nominated organisation already been registered")
    )
    when(organisationAlreadyInGroupPage.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("error organisation already in group")
    )
    when(groupMemberAlreadyRegisteredPage.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("error group member already registered")
    )

  }

  "NotableErrorController nominatedOrganisationAlreadyRegistered" should {

    "present nominated organisation already been registered page" in {
      spyJourneyAction.setReg(aRegistration())

      val resp = controller.nominatedOrganisationAlreadyRegistered()(FakeRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error nominated organisation already been registered"
    }
  }

  "NotableErrorController organisationAlreadyInGroup" should {

    "present organisation already in group page" when {
      "registration has group error" in {

        val registration = aRegistration(
          withGroupDetail(Some(GroupDetail(groupError = Some(GroupError(MEMBER_IN_GROUP, "Member Name")))))
        )
        spyJourneyAction.setReg(registration)

        val resp = controller.organisationAlreadyInGroup()(FakeRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "error organisation already in group"
      }
    }

    "redirect to organisation list" when {
      "registration does not have group error" in {
        spyJourneyAction.setReg(aRegistration())

        val resp = controller.organisationAlreadyInGroup()(FakeRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(groupRoutes.OrganisationListController.displayPage().url)
      }
    }
  }

  "NotableErrorController groupMemberAlreadyRegistered" should {

    "present group member already been registered page" in {

      val registration = aRegistration(
        withGroupDetail(Some(GroupDetail(groupError = Some(GroupError(MEMBER_IS_ALREADY_REGISTERED, "Member Name")))))
      )
      spyJourneyAction.setReg(registration)

      val resp = controller.groupMemberAlreadyRegistered()(FakeRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error group member already registered"
    }
  }
}
