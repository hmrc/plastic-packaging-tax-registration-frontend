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

package views.liability

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import controllers.liability.{
  routes => liabilityRoutes
}
import views.html.liability.not_members_under_group_control_page

class NotMembersUnderGroupControlPageViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[not_members_under_group_control_page]

  private def createView(): Document =
    page()(registrationJourneyRequest, messages)

  "Cannot apply as a group view" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        liabilityRoutes.MembersUnderGroupControlController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("group.notMembersUnderGroupControl.title"))
    }

    "display detail" in {

      view.getElementsByClass(
        "govuk-body"
      ).text() mustBe "You can apply as a single organisation or partnership."
    }

    "display 'Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Continue"
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
