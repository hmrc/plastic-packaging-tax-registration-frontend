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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.GroupDetail
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupError
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupErrorType.MEMBER_IN_GROUP
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.{
  nominated_organisation_already_registered_page,
  organisation_already_in_group_page
}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class NotableErrorControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val nominatedOrganisationAlreadyRegisteredPage =
    mock[nominated_organisation_already_registered_page]

  private val organisationAlreadyInGroupPage = mock[organisation_already_in_group_page]

  private val controller =
    new NotableErrorController(authenticate = mockAuthAction,
                               mockJourneyAction,
                               mcc = mcc,
                               nominatedOrganisationAlreadyRegisteredPage =
                                 nominatedOrganisationAlreadyRegisteredPage,
                               organisationAlreadyInGroupPage = organisationAlreadyInGroupPage
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(nominatedOrganisationAlreadyRegisteredPage.apply()(any(), any())).thenReturn(
      HtmlFormat.raw("error nominated organisation already been registered")
    )
    when(organisationAlreadyInGroupPage.apply(any())(any(), any())).thenReturn(
      HtmlFormat.raw("error organisation already in group")
    )
  }

  "NotableErrorController nominatedOrganisationAlreadyRegistered" should {

    "present nominated organisation already been registered page" in {
      authorizedUser()
      val resp = controller.nominatedOrganisationAlreadyRegistered()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error nominated organisation already been registered"
    }
  }

  "NotableErrorController organisationAlreadyInGroup" should {

    "present organisation already in group page" when {
      "registration has group error" in {
        authorizedUser()
        val registration = aRegistration(
          withGroupDetail(
            Some(GroupDetail(groupError = Some(GroupError(MEMBER_IN_GROUP, "Member Name"))))
          )
        )
        mockRegistrationFind(registration)

        val resp = controller.organisationAlreadyInGroup()(getRequest())

        status(resp) mustBe OK
        contentAsString(resp) mustBe "error organisation already in group"
      }
    }

    "redirect to organisation list" when {
      "registration does not have group error" in {
        authorizedUser()
        val resp = controller.organisationAlreadyInGroup()(getRequest())

        status(resp) mustBe SEE_OTHER
        redirectLocation(resp) mustBe Some(groupRoutes.OrganisationListController.displayPage().url)
      }
    }
  }
}
