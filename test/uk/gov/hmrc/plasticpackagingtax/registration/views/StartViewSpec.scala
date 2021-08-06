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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.config.Features
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.start_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest
import utils.FakeRequestCSRFSupport._

@ViewTest
class StartViewSpec extends UnitViewSpec with Matchers {

  private val startPage = instanceOf[start_page]

  private def createView(): Html = startPage()(request, messages)

  "Start Page view" should {

    "have proper messages for labels" in {

      messages must haveTranslationFor("startPage.title.sectionHeader")
      messages must haveTranslationFor("startPage.title")
      messages must haveTranslationFor("startPage.description")
      messages must haveTranslationFor("startPage.hmrcUrl")
      messages must haveTranslationFor("startPage.information")
      messages must haveTranslationFor("startPage.contents.header")
      messages must haveTranslationFor("startPage.useThisServiceTo.header")
      messages must haveTranslationFor("startPage.useThisServiceTo.listItem.1")
      messages must haveTranslationFor("startPage.useThisServiceTo.listItem.2")
      messages must haveTranslationFor("startPage.useThisServiceTo.listItem.3")
      messages must haveTranslationFor("startPage.useThisServiceTo.listItem.4")
      messages must haveTranslationFor("startPage.overview.header")
      messages must haveTranslationFor("startPage.overview.line.1")
      messages must haveTranslationFor("startPage.overview.line.2")
      messages must haveTranslationFor("startPage.overview.line.3")
      messages must haveTranslationFor("startPage.informationYouNeed.header")
      messages must haveTranslationFor("startPage.informationYouNeed.line.1")
      messages must haveTranslationFor("startPage.informationYouNeed.listItem.1")
      messages must haveTranslationFor("startPage.informationYouNeed.listItem.2")
      messages must haveTranslationFor("startPage.informationYouNeed.listItem.3")
      messages must haveTranslationFor("startPage.makeDeclaration.header")
      messages must haveTranslationFor("startPage.buttonName")
    }

    val view: Html = createView()

    "not contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe false

    }

    "display title" in {

      view.select("title").text() must include(messages("startPage.title"))
    }

    "display section header" in {

      view.getElementById("section-header") must containMessage("startPage.title.sectionHeader")
    }

    "display header" in {

      view.getElementById("title") must containMessage("startPage.title")
    }

    "display general description" in {

      view.getElementById("description") must containMessage("startPage.description")
    }

    "display Contents section" in {

      view.getElementsByClass("dashed-list-title").get(0) must containMessage(
        "startPage.contents.header"
      )

      val contentList = view.getElementsByClass("dashed-list").get(0)

      contentList must haveChildCount(3)
      contentList.child(0) must containMessage("startPage.overview.header")
      contentList.child(1) must containMessage("startPage.informationYouNeed.header")
      contentList.child(2) must containMessage("startPage.makeDeclaration.header")
    }

    "contain links in Contents section" in {

      val contentList = view.getElementsByClass("dashed-list").get(0)

      contentList.child(0).child(0) must haveHref("#overview")
      contentList.child(1).child(0) must haveHref("#information-you-need")
      contentList.child(2).child(0) must haveHref("#make-declaration")
    }

    "display 'Use this service to' section" in {

      view.getElementsByClass("govuk-body").get(0) must containMessage(
        "startPage.useThisServiceTo.header"
      )

      val useThisServiceList = view.getElementsByClass("govuk-list--bullet").get(0)

      useThisServiceList must haveChildCount(4)
      useThisServiceList.child(0) must containMessage("startPage.useThisServiceTo.listItem.1")
      useThisServiceList.child(1) must containMessage("startPage.useThisServiceTo.listItem.2")
      useThisServiceList.child(2) must containMessage("startPage.useThisServiceTo.listItem.3")
      useThisServiceList.child(3) must containMessage("startPage.useThisServiceTo.listItem.4")
    }

    "display 'Overview' section" in {

      view.getElementById("overview") must containMessage("startPage.overview.header")

      view.getElementById("overview-element-1").text() must include(
        messages("startPage.overview.line.1")
      )
      view.getElementById("overview-element-2").text() must include(
        messages("startPage.overview.line.2")
      )
      view.getElementById("overview-element-3").text() must include(
        messages("startPage.overview.line.3")
      )
    }

    "display 'Information you need' section" in {

      view.getElementById("information-you-need") must containMessage(
        "startPage.informationYouNeed.header"
      )

      view.getElementsByClass("govuk-body").get(4) must containMessage(
        "startPage.informationYouNeed.line.1"
      )

      val informationYouNeedList = view.getElementsByClass("govuk-list--bullet").get(1)

      informationYouNeedList must haveChildCount(3)
      informationYouNeedList.child(0) must containMessage("startPage.informationYouNeed.listItem.1")
      informationYouNeedList.child(1) must containMessage("startPage.informationYouNeed.listItem.2")
      informationYouNeedList.child(2) must containMessage("startPage.informationYouNeed.listItem.3")
    }

    "display 'Make a declaration' section" in {

      view.getElementById("make-declaration") must containMessage(
        "startPage.makeDeclaration.header"
      )
    }

    "display 'Start now' button" in {

      view.getElementsByClass("govuk-button").first() must containMessage("startPage.buttonName")
      view.getElementsByClass("govuk-button").first() must haveHref(
        uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes.LiabilityWeightController.displayPage().url
      )
    }

    "display link to go back to Contents section" in {

      view.getElementById("back-to-top") must containMessage("startPage.contents.header")
      view.getElementById("back-to-top").child(0) must haveHref("#contents")
    }
  }
}
