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

package views.amendment.group

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import forms.group.RemoveMember
import forms.group.RemoveMember.form
import views.html.amendment.group.confirm_remove_member_page

class ConfirmRemoveMemberPageSpec extends UnitViewSpec with Matchers {

  private val page = inject[confirm_remove_member_page]

  private def createView(form: Form[RemoveMember] = RemoveMember.form()): Document =
    page(form, groupMember)(journeyRequest, messages)

  "Confirm Remove Member  page" should {

    val view = createView()

    "contain title" in {
      view.select("title").text() must include(
        messages("amend.group.remove.title", groupMember.businessName)
      )
    }

    "contain heading" in {
      view.select("h1").text() mustBe messages("amend.group.remove.title", groupMember.businessName)
    }

    "contain radio groups with yes option" in {
      view must containElementWithID("value")
      val yesRadio = view.getElementById("value")
      yesRadio.attr("type") mustBe "radio"
      yesRadio.attr("value") mustBe "yes"
    }

    "display 'Continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Continue"
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(form(), groupMember)(journeyRequest, messages)
    page.render(form(), groupMember, journeyRequest, messages)
  }

}
