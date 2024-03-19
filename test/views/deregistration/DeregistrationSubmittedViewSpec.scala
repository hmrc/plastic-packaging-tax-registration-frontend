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

package views.deregistration

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import config.AppConfig
import views.html.deregistration.deregistration_submitted_page

class DeregistrationSubmittedViewSpec extends UnitViewSpec with Matchers {

  val appConfig: AppConfig = inject[AppConfig]
  private val page         = inject[deregistration_submitted_page]

  "The Deregistration Submitted View" should {

    val view = page()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display title" in {
      view.select("title").text() must include(messages("deregistration.confirmation.title"))
    }

    "display sub-heading" in {
      view.select("h2").text() must include(messages("deregistration.confirmation.subheading"))
    }

    "display confirmation panel" in {
      view.select("h1").text() must include(messages("deregistration.confirmation.title"))
    }

    "display page detail" in {
      val mainContent = view.select("main").text()
      mainContent must include(messages("deregistration.confirmation.detail"))
      mainContent must include(messages("deregistration.confirmation.detail1"))
      mainContent must include(
        messages("deregistration.confirmation.detail2", messages("deregistration.confirmation.detail2.link"))
      )

      val mainContentLink = view.select("main a").get(0)
      mainContentLink must haveHref(appConfig.pptAccountUrl)
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
