/*
 * Copyright 2024 HM Revenue & Customs
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

package views.group

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import models.registration.group.GroupError
import models.registration.group.GroupErrorType.MEMBER_IS_ALREADY_REGISTERED
import views.components.Styles.gdsPageBodyText
import views.html.group.group_member_already_registered_page

class GroupMemberAlreadyRegisteredViewSpec extends UnitViewSpec with Matchers {

  private val page: group_member_already_registered_page =
    inject[group_member_already_registered_page]

  private val groupError = GroupError(MEMBER_IS_ALREADY_REGISTERED, "Plastic Packaging Ltd")

  private def createView(): Html = page(groupError)(registrationJourneyRequest, messages)

  "Group member Already Registered Page" should {

    val view: Html = createView()

    "display title" in {
      view.select("title").text() must include(messages("group.member.already.registered.title", "Plastic Packaging Ltd"))
    }

    "display heading" in {
      view.select("h1").text() must include(messages("group.member.already.registered.title", "Plastic Packaging Ltd"))
    }

    "display detail" in {
      view.select("p.govuk-body").text() must include(messages("group.member.already.registered.detail1", "Plastic Packaging Ltd"))
      val details = view.getElementsByClass(gdsPageBodyText)
      details.get(1) must containMessage("group.member.already.registered.detail2", "Plastic Packaging Ltd")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(groupError)(registrationJourneyRequest, messages)
    page.render(groupError, registrationJourneyRequest, messages)
  }

}
