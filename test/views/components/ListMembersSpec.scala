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

package views.components

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import play.twirl.api.Html
import views.html.components.listMembers
import views.viewmodels.ListMember

class ListMembersSpec extends UnitViewSpec with Matchers {

  private val singleMember = Seq(ListMember(name = "Test Member"))

  protected val component: listMembers =
    new listMembers()

  "List Members Component" should {
    "render list members" when {
      "single member" in {
        val view: Html = component(singleMember)(messages)

        val items = view.getElementsByClass("hmrc-add-to-a-list__contents")
        items.size() mustBe 1
        items.get(0).getElementsByClass(
          "hmrc-add-to-a-list__identifier"
        ).text() mustBe "Test Member"
      }

      "multiple members" in {
        val view: Html =
          component(Seq(ListMember(name = "Member1"), ListMember(name = "Member2")))(messages)

        val items = view.getElementsByClass("hmrc-add-to-a-list__contents")
        items.size() mustBe 2
        items.get(0).getElementsByClass("hmrc-add-to-a-list__identifier").text() mustBe "Member1"
        items.get(1).getElementsByClass("hmrc-add-to-a-list__identifier").text() mustBe "Member2"
      }

      "has optional sub-heading" in {
        val view: Html =
          component(Seq(ListMember(name = "MemberName", subHeading = Some("SubHeading"))))(messages)

        val items = view.getElementsByClass("hmrc-add-to-a-list__contents")
        items.size() mustBe 1
        items.get(0).getElementsByClass(
          "hmrc-add-to-a-list__identifier"
        ).text() mustBe "MemberName SubHeading"
      }

      "has optional change link" in {
        val view: Html = component(
          Seq(ListMember(name = "Name", change = Some(Call("GET", "/change-url"))))
        )(messages)

        view.select(
          ".hmrc-add-to-a-list__contents > .hmrc-add-to-a-list__change > a"
        ).first() must haveHref("/change-url")
      }

      "has optional remove link" in {
        val view: Html = component(
          Seq(ListMember(name = "Name", remove = Some(Call("GET", "/remove-url"))))
        )(messages)

        view.select(
          ".hmrc-add-to-a-list__contents > .hmrc-add-to-a-list__remove > a"
        ).first() must haveHref("/remove-url")
      }

      "not render links" in {
        val view: Html = component(singleMember)(messages)

        view.select(
          ".hmrc-add-to-a-list__contents > .hmrc-add-to-a-list__change > a"
        ) must haveSize(0)

        view.select(
          ".hmrc-add-to-a-list__contents > .hmrc-add-to-a-list__remove > a"
        ) must haveSize(0)
      }
    }
    "not render line separator" when {
      "remove link is not rendered" in {
        val view: Html = component(singleMember)(messages)

        view.select(".hmrc-add-to-a-list__contents > .hmrc-add-to-a-list__remove") must haveSize(0)
      }
    }

    "render line separator" when {
      "remove link is rendered" in {
        val view: Html = component(
          Seq(ListMember(name = "any name", remove = Some(Call("GET", "/remove-url"))))
        )(messages)

        view.select(".hmrc-add-to-a-list__contents > .hmrc-add-to-a-list__remove") must haveSize(1)
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    component.f(singleMember)
    component.render(singleMember, messages)
  }

}
