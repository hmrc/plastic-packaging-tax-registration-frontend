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

package uk.gov.hmrc.plasticpackagingtax.registration.views.amendment.group

import base.unit.UnitViewSpec
import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import play.api
import play.api.Application
import play.api.data.Form
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.AddOrganisation
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.UK_COMPANY
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{OrganisationDetails, Registration}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.list_group_members_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.components.{listMembers, saveButtons}
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.ListMember
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.group.AddOrganisation._

class ListGroupMembersPageSpec extends UnitViewSpec with Matchers with Injecting with BeforeAndAfterEach {

  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear() //todo turn jvm metrics off for local.
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[listMembers].toInstance(mockListComponent),
        api.inject.bind[saveButtons].toInstance(mockSaveButton)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockListComponent, mockSaveButton)
  }

  val mockListComponent: listMembers = mock[listMembers]
  val mockSaveButton: saveButtons = mock[saveButtons]
  lazy val view: list_group_members_page = inject[list_group_members_page]
  lazy val realAppConfig: AppConfig = inject[AppConfig]

  val registration: Registration = Registration("someID",
    organisationDetails = OrganisationDetails(organisationType = Some(UK_COMPANY), incorporationDetails = Some(incorporationDetails)),
    groupDetail = Some(groupDetails.copy(members = Seq(groupMember, groupMember)))
  )
  val form: Form[AddOrganisation] = AddOrganisation.form()


  "manage_group_members_page" must {
    "have the back button" in {
      val sut: HtmlFormat.Appendable = view(form, registration)(journeyRequest, messages)

      sut.getElementById("back-link") must haveHref(realAppConfig.pptAccountUrl)
    }

    "contain title" in {
      val sut: HtmlFormat.Appendable = view(form, registration)(journeyRequest, messages)

      sut.select("title").text() must include(messages("amend.group.listMembers.title", 2))
    }

    "contain heading" in {
      val sut: HtmlFormat.Appendable = view(form, registration)(journeyRequest, messages)

      sut.select("h1").text() mustBe messages("amend.group.listMembers.title", 2)
    }

    "list the members" in {
      when(mockListComponent.apply(any())(any())).thenReturn(Html("listed members component"))

      val sut: HtmlFormat.Appendable = view(form, registration)(journeyRequest, messages)

      val representativeMember = ListMember(
        name = registration.organisationDetails.businessName.get,
        subHeading = Some(messages("amend.group.manage.representativeMember")),
        change = None
      )
      val listMember = ListMember(
        groupMember.businessName,
        change = Some(groupRoutes.ContactDetailsCheckAnswersController.displayPage(groupMember.id)),
        remove = Some(routes.ConfirmRemoveMemberController.displayPage(groupMember.id))
      )

      verify(mockListComponent).apply(refEq(Seq(representativeMember, listMember, listMember)))(any())
      sut.toString() must include("listed members component")
    }

    "display add member form" in {
      when(mockSaveButton.apply(any(), any())(any())).thenReturn(Html("save button"))
      val sut: HtmlFormat.Appendable = view(form, registration)(journeyRequest, messages)

      sut.select("form").attr("method") mustBe "POST"
      sut.select("form").attr("action") mustBe routes.GroupMembersListController.onSubmit().url
      sut.select("legend").text() mustBe messages("addOrganisation.add.heading")
      sut.getElementById("addOrganisation").attr("value")  mustBe YES
      sut.getElementById("addOrganisation-2").attr("value")  mustBe NO

      withClue("should have the save button"){
        verify(mockSaveButton).apply(any(), any())(any())
        sut.toString() must include("save button")
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    view.f(form, registration)(journeyRequest, messages)
    view.render(form, registration, journeyRequest, messages)
  }
}
