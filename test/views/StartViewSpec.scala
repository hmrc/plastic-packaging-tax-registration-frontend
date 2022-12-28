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
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import views.html.start_page

class StartViewSpec extends UnitViewSpec with Matchers {

  private val startPage = inject[start_page]

  private def createView(): Html =
    startPage()(request, messages)

  "Start Page view" should {

    val view: Html = createView()

    "not contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe false
    }

    "display title" in {
      view.select("title").text() must include(messages("startPage.title"))
    }

    "display header" in {
      view.getElementsByTag("h1") must containMessageForElements("startPage.heading")
    }

    "display general description" in {
      view.getElementById("description") must containMessage("startPage.description")
    }

    "display 'Who should register' section" in {

      view.getElementById("who-should-register") must containMessage("startPage.whoShouldRegister")

      view.getElementsByClass("govuk-body").text() must include(
        messages("startPage.whoShouldRegister.inset.para.1")
      )

      val bulletList = view.getElementsByClass("govuk-list--bullet").get(0)

      bulletList.child(0) must containMessage("startPage.whoShouldRegister.inset.bullet.1")
      bulletList.child(1) must containMessage("startPage.whoShouldRegister.inset.bullet.2")
    }

    "display the read more link" in {
      val link = view.getElementById("whoShouldRegister-readMore-link")

      link must containMessage("startPage.whoShouldRegister.readMore.link")
      link must haveHref(messages("startPage.whoShouldRegister.readMore.href"))
      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
    }

    "display 'Before you start' section" in {

      view.getElementById("before-you-start") must containMessage("startPage.beforeYouStart")

      view.getElementsByClass("govuk-body").text() must include(
        messages("startPage.beforeYouStart.para.1")
      )

      val bulletList = view.getElementsByClass("govuk-list--bullet").get(1)

      bulletList.child(0).text() must include(
        messages("startPage.beforeYouStart.bullet.1",
                 messages("startPage.beforeYouStart.bullet.1.link")
        )
      )
      bulletList.child(1) must containMessage("startPage.beforeYouStart.bullet.2")
      bulletList.child(2) must containMessage("startPage.beforeYouStart.bullet.3")
      bulletList.child(3) must containMessage("startPage.beforeYouStart.bullet.4")
    }

    "display company look up link" in {
      val link = view.getElementById("beforeYouStart-bullet-1-link")

      link must containMessage("startPage.beforeYouStart.bullet.1.link")
      link must haveHref(messages("startPage.beforeYouStart.bullet.1.href"))
      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
    }

    "display 'Start now' button" in {

      view.getElementsByClass("govuk-button").first() must containMessage("startPage.buttonName")
      view.getElementsByClass("govuk-button").first() must haveHref(
        controllers.routes.StartRegistrationController.startRegistration()
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    startPage.f()(request, messages)
    startPage.render(request, messages)
  }

}
