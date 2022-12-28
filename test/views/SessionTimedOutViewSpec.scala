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

package views

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import controllers.routes
import views.components.Styles
import views.html.session_timed_out

class SessionTimedOutViewSpec extends UnitViewSpec with Matchers {

  private val page                   = inject[session_timed_out]
  private def createView(): Document = page()(request, messages)

  "Session Timeout View" should {

    val view = createView()

    "not contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe false
    }

    "display title" in {

      view.select("title").first() must containMessage("sessionTimout.title")
    }

    "display heading" in {

      view.getElementsByClass(Styles.gdsPageHeading).first() must containMessage(
        "sessionTimout.title"
      )
    }

    "display detail" in {

      view.getElementsByClass("govuk-body").first() must containMessage("sessionTimout.detail")
    }

    "display 'Sign in' button" in {

      view must containElementWithClass("govuk-button")
      view.getElementsByClass("govuk-button").first() must containMessage(
        "sessionTimout.signin.button"
      )
      view.getElementsByClass("govuk-button").first() must haveHref(
        routes.TaskListController.displayPage().url
      )
    }

    "display 'back to gov' link" in {

      view must containElementWithID("govuk-link")
      view.getElementById("govuk-link") must containMessage("site.backToGovUk")
      view.getElementById("govuk-link") must haveHref("https://www.gov.uk")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
