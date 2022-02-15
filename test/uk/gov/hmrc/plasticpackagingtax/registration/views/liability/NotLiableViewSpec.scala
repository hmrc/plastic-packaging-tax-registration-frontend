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

package uk.gov.hmrc.plasticpackagingtax.registration.views.liability

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.not_liable
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class NotLiableViewSpec extends UnitViewSpec with Matchers {

  private val page = inject[not_liable]

  private def createView(): Document =
    page()(journeyRequest, messages)

  "Not Liable View" should {

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
      val link = view.getElementById("guidance-link")

      link must haveHref(messages("notLiable.guidance.link.href"))

      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
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

      when(appConfig.feedbackAuthenticatedLink).thenReturn(
        "http://localhost:9250/contact/beta-feedback"
      )
      view.getElementById("feedback-link") must haveHref(
        "http://localhost:9250/contact/beta-feedback?service=plastic-packaging-tax&backUrl=http://localhost:8503/"
      )
    }

    "display feedback link for unauthenticated users" in {

      val unauthenticatedView = page()(FakeRequest(), messages)
      unauthenticatedView.getElementById("feedback-link") must haveHref(
        "http://localhost:9250/contact/beta-feedback-unauthenticated?service=plastic-packaging-tax&backUrl=http://localhost:8503/"
      )
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
