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
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.session_timed_out
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest
import utils.FakeRequestCSRFSupport._

@ViewTest
class SessionTimedOutViewSpec extends UnitViewSpec with Matchers {

  override implicit val request: Request[AnyContent] = FakeRequest().withCSRFToken

  private val page                   = instanceOf[session_timed_out]
  private def createView(): Document = page()(request, messages)

  "Session Timeout View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("sessionTimout.title")
      messages must haveTranslationFor("sessionTimout.paragraph.saved")
      messages must haveTranslationFor("sessionTimout.signin.button")
      messages must haveTranslationFor("site.backToGovUk")

    }

    val view = createView()

    "not contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe false
    }

    "display title" in {

      view.getElementsByClass("govuk-heading-xl").first() must containMessage("sessionTimout.title")
    }

    "display saved answers info" in {

      view.getElementsByClass("govuk-body").first() must containMessage(
        "sessionTimout.paragraph.saved"
      )
    }

    "display 'Sign in' button" in {

      view must containElementWithClass("govuk-button")
      view.getElementsByClass("govuk-button").first() must containMessage(
        "sessionTimout.signin.button"
      )
      view.getElementsByClass("govuk-button").first() must haveHref(
        routes.RegistrationController.displayPage().url
      )
    }

    "display 'back to gov' link" in {

      view must containElementWithID("govuk-link")
      view.getElementById("govuk-link") must containMessage("site.backToGovUk")
      view.getElementById("govuk-link") must haveHref("https://www.gov.uk")
    }
  }
}
