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
import play.api.mvc.Flash
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.models.response.FlashKeys
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.confirmation_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ConfirmationViewSpec extends UnitViewSpec with Matchers {

  private val page: confirmation_page = instanceOf[confirmation_page]

  private def createView(flash: Flash = new Flash(Map.empty)): Html =
    page()(journeyRequest, messages, flash)

  "Confirmation Page view" should {

    "validate other rendering methods" in {
      page.f()(request, messages, new Flash(Map.empty))
      page.render(request, messages, new Flash(Map.empty))
    }

    "have proper messages for labels" in {

      messages must haveTranslationFor("confirmationPage.title")
      messages must haveTranslationFor("confirmationPage.panel.body")
      messages must haveTranslationFor("confirmationPage.body")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.title")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.paragraph1")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.liable.title")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.liable.bulletPoint.1")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.liable.bulletPoint.2")
      messages must haveTranslationFor("confirmationPage.exitSurvey.text.link")
      messages must haveTranslationFor("confirmationPage.exitSurvey.text")
      messages must haveTranslationFor("confirmationPage.enrolment.initiationSuccess")
      messages must haveTranslationFor("confirmationPage.enrolment.link.text")
      messages must haveTranslationFor("confirmationPage.enrolment.initiationFailure")
    }

    val view: Html = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("confirmationPage.title"))
    }

    "display link to ppt accounts" when {

      "flash contains successful enrolment " in {

        val successView = createView(new Flash(Map("enrolmentSuccessful" -> "true")))
        successView.getElementById("ppt-account-link") must containMessage(
          "confirmationPage.enrolment.link.text"
        )
      }

    }

    "display panel" when {

      "no 'referenceId' has been provided" in {
        view.getElementsByClass("govuk-panel__title").get(0) must containMessage(
          "confirmationPage.title"
        )
        view.getElementsByClass("govuk-panel__body").get(0) must containMessage(
          "confirmationPage.panel.body.default"
        )
      }

      "a 'referenceId' has been provided" in {
        val viewWithReferenceId = createView(flash = Flash(Map(FlashKeys.referenceId -> "PPT123")))
        viewWithReferenceId.getElementsByClass("govuk-panel__title").get(0) must containMessage(
          "confirmationPage.title"
        )
        viewWithReferenceId.getElementsByClass("govuk-panel__body").get(0) must containMessage(
          "confirmationPage.panel.body",
          "PPT123"
        )
      }
    }

    "display body" in {

      view.getElementsByClass("govuk-body").get(0) must containMessage("confirmationPage.body")
    }

    "display enrolment status" in {

      view.getElementsByClass("govuk-body").get(1) must containMessage(
        "confirmationPage.enrolment.initiationFailure"
      )
    }

    "display 'What happens next'" in {

      view.getElementsByClass("govuk-heading-m").get(0) must containMessage(
        "confirmationPage.whatHappensNext.title"
      )
      view.getElementsByClass("govuk-body").get(2) must containMessage(
        "confirmationPage.whatHappensNext.paragraph1"
      )
      view.getElementsByClass("govuk-body").get(3) must containMessage(
        "confirmationPage.whatHappensNext.liable.title"
      )
      view.getElementsByClass("dashed-list-item").get(0) must containMessage(
        "confirmationPage.whatHappensNext.liable.bulletPoint.1"
      )
      view.getElementsByClass("dashed-list-item").get(1) must containMessage(
        "confirmationPage.whatHappensNext.liable.bulletPoint.2"
      )

      view.getElementsByClass("govuk-body").get(4) must containMessage(
        "confirmationPage.exitSurvey.text",
        messages("confirmationPage.exitSurvey.text.link")
      )
    }

  }
}
