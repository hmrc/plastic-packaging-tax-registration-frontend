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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.not_liable
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig

@ViewTest
class NotLiableViewSpec extends UnitViewSpec with Matchers {

  private val page      = instanceOf[not_liable]
  private val appConfig = instanceOf[AppConfig]

  private def createView(): Document =
    page()(request, messages)

  "Not Liable View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("notLiable.pageTitle")
      messages must haveTranslationFor("notLiable.title")
      messages must haveTranslationFor("notLiable.inset")
      messages must haveTranslationFor("notLiable.guidance")
      messages must haveTranslationFor("notLiable.guidance.link.description")
      messages must haveTranslationFor("notLiable.guidance.link.href")
      messages must haveTranslationFor("notLiable.think.title")
      messages must haveTranslationFor("notLiable.think.info")
      messages must haveTranslationFor("notLiable.think.feedback")
      messages must haveTranslationFor("notLiable.think.feedback.link.description")
    }

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display title" in {

      view.select("title").text() must include(messages("notLiable.pageTitle"))
    }

    "display page heading" in {

      view.getElementById("page-heading").text() must include(messages("notLiable.title"))
    }

    "display need for recording warning" in {

      view.getElementById("records-warning").text() must include(messages("notLiable.inset"))
    }

    "display guidance text" in {

      view.getElementById("guidance-text").text() must include(
        messages("notLiable.guidance", messages("notLiable.guidance.link.description"))
      )
    }

    "display guidance link" in {

      view.getElementById("guidance-link") must haveHref(messages("notLiable.guidance.link.href"))
    }

    "display feedback subheading" in {

      view.getElementById("feedback-heading").text must include(messages("notLiable.think.title"))
    }

    "display feedback text" in {

      view.getElementById("feedback-text1").text must include(messages("notLiable.think.info"))
      view.getElementById("feedback-text2").text must include(
        messages("notLiable.think.feedback", messages("notLiable.think.feedback.link.description"))
      )
    }

    "display feedback link for authenticated users" in {

      view.getElementById("feedback-link") must haveHref(
        s"${appConfig.authenticatedFeedbackUrl()}&backUrl=${appConfig.selfBaseUrl}/"
      )
    }

    "display feedback link for unauthenticated users" in {

      val unauthenticatedView = page()(FakeRequest(), messages)
      unauthenticatedView.getElementById("feedback-link") must haveHref(
        s"${appConfig.unauthenticatedFeedbackUrl()}&backUrl=${appConfig.selfBaseUrl}/"
      )
    }
  }
}