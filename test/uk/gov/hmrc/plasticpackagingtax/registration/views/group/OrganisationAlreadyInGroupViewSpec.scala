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

package uk.gov.hmrc.plasticpackagingtax.registration.views.group

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.group.{routes => groupRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupError
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.group.GroupErrorType.{
  MEMBER_IN_GROUP,
  MEMBER_IS_NOMINATED
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.organisation_already_in_group_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class OrganisationAlreadyInGroupViewSpec extends UnitViewSpec with Matchers {

  private val page: organisation_already_in_group_page =
    instanceOf[organisation_already_in_group_page]

  private val groupError = GroupError(MEMBER_IN_GROUP, "Member Name")

  private def createView(groupError: GroupError = groupError): Html =
    page(groupError)(journeyRequest, messages)

  "Organisation Already In Group Page" should {

    val view: Html = createView()

    "display title" in {
      view.select("title").text() must include(
        messages("organisation.already.in.group.title", "Member Name")
      )
    }

    "display heading" in {
      view.select("h1").text() must include(
        messages("organisation.already.in.group.title", "Member Name")
      )
    }

    "display save and continue link-button" in {
      view.select("a.govuk-button").text() must include(messages("site.button.saveAndContinue"))
      view.select("a.govuk-button").first() must haveHref(
        groupRoutes.OrganisationListController.displayPage()
      )
    }

    "display detail" when {
      "nominated member is already group" in {
        val view = createView(GroupError(MEMBER_IS_NOMINATED, "Nominated Member"))
        view.select("p.govuk-body").text() must include(
          messages("organisation.already.in.group.detail.nominated", "Nominated Member")
        )
      }

      "member is already group" in {
        val view = createView(GroupError(MEMBER_IN_GROUP, "Member"))
        view.select("p.govuk-body").text() must include(
          messages("organisation.already.in.group.detail.member", "Member")
        )
      }
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(groupError)(journeyRequest, messages)
    page.render(groupError, journeyRequest, messages)
  }

}
