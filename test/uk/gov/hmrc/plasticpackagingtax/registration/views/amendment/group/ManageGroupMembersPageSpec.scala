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
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers
import play.api.test.Injecting
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.amendment.group.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.OrgType.UK_COMPANY
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.amendment.group.manage_group_members_page

class ManageGroupMembersPageSpec extends UnitViewSpec with Matchers with Injecting {

  val view: manage_group_members_page = inject[manage_group_members_page]
  val realAppConfig: AppConfig        = inject[AppConfig]

  val registration: Registration = Registration("someID",
                                                organisationDetails = OrganisationDetails(
                                                  organisationType = Some(UK_COMPANY),
                                                  incorporationDetails = Some(incorporationDetails)
                                                ),
                                                groupDetail = Some(
                                                  groupDetails.copy(members =
                                                    Seq(groupMember, groupMember)
                                                  )
                                                )
  )

  val sut: HtmlFormat.Appendable = view(registration)(journeyRequest, messages)

  "manage_group_members_page" must {
    "have the back button" in {
      sut.getElementById("back-link") must haveHref(realAppConfig.pptAccountUrl)
    }

    "contain title" in {
      sut.select("title").text() must include(messages("amend.group.manage.title"))
    }

    "contain heading" in {
      sut.select("h1").text() mustBe messages("amend.group.manage.title")
    }

    "display representative organisation details" in {
      val key   = sut.select("dt").get(0).text()
      val value = sut.select("dd").get(0).text()

      key must include(messages("amend.group.manage.representativeMember"))
      value mustBe incorporationDetails.companyName
    }

    "display the other organisations" in {
      val key        = sut.select("dt").get(1).text()
      val value      = sut.select("dd").get(1).text()
      val changeLink = sut.select("dd").get(2).select("a").get(0)

      key must include(messages("amend.group.manage.members"))
      value mustBe s"${groupMember.businessName} ${groupMember.businessName}"
      changeLink.text() mustBe messages("site.link.change") + " " + messages(
        "amend.group.manage.members"
      )
      changeLink must haveHref(routes.GroupMembersListController.displayPage())
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    view.f(registration)(journeyRequest, messages)
    view.render(registration, journeyRequest, messages)
  }

}