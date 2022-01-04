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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.start_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class StartViewSpec extends UnitViewSpec with Matchers {

  private val startPage = instanceOf[start_page]

  private def createView(): Html = startPage()(request, messages)

  "Start Page view" should {

    val view: Html = createView()

    "not contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe false
    }

    "display title" in {

      view.select("title").text() must include(messages("startPage.title"))
    }

    "display header" in {

      view.getElementsByTag("h1") must containMessageForElements("startPage.title")
    }

    "display general description" in {

      view.getElementById("description") must containMessage("startPage.description")
    }

    "display 'When to register' section" in {

      view.getElementById("when-to-register") must containMessage("startPage.whenToRegister")

      view.getElementsByClass("govuk-body").text() must include(
        messages("startPage.whenToRegister.line.1",
                 "startPage.whenToRegister.line.2",
                 "startPage.registration.guidance.description"
        )
      )
    }

    "display 'Before you start' section" in {

      view.getElementById("before-you-start") must containMessage("startPage.beforeYouStart")

      view.getElementsByClass("govuk-body").text() must include(
        messages("startPage.beforeYouStart.line.1",
                 "startPage.beforeYouStart.line.2",
                 "startPage.business.guidance.description"
        )
      )
    }

    "display bullet list section" in {

      view.getElementsByClass("govuk-body").get(5).text() must include(
        messages("startPage.informationYouNeed")
      )

      val bulletList = view.getElementsByClass("govuk-list--bullet").get(0)

      bulletList must haveChildCount(4)
      bulletList.child(0).text() must include(
        messages("startPage.informationYouNeed.listItem.1",
                 "look up the company number if you are unsure (opens in a new tab)"
        )
      )
      bulletList.child(1) must containMessage("startPage.informationYouNeed.listItem.2")
      bulletList.child(2) must containMessage("startPage.informationYouNeed.listItem.3")
      bulletList.child(3) must containMessage("startPage.informationYouNeed.listItem.4")
    }

    "display registration guidance link" in {

      val link = view.getElementById("registration-guidance-link")
      link must haveHref(messages("startPage.registration.guidance.href"))
      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
    }

    "display business guidance link" in {

      val link = view.getElementById("business-guidance-link")
      link must haveHref(messages("startPage.business.guidance.href"))
      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
    }

    "display company number link" in {

      val link = view.getElementById("company-number-link")
      link must haveHref(messages("startPage.companyNumber.href"))
      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
    }

    "display 'Start now' button" in {

      view.getElementsByClass("govuk-button").first() must containMessage("startPage.buttonName")
      view.getElementsByClass("govuk-button").first() must haveHref(
        uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes.StartRegistrationController.startRegistration().url
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    startPage.f()(request, messages)
    startPage.render(request, messages)
  }

}
