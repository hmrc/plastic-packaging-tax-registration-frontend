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
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.confirmation_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ConfirmationViewSpec extends UnitViewSpec with Matchers {

  private val page: confirmation_page = instanceOf[confirmation_page]

  private def createView(flash: Flash = new Flash(Map.empty)): Html =
    page()(journeyRequest, messages, flash)

  "Confirmation Page view" should {

    "have proper messages for labels" in {

      messages must haveTranslationFor("confirmationPage.title")
      messages must haveTranslationFor("confirmationPage.panel.body")
      messages must haveTranslationFor("confirmationPage.panel.body.default")
      messages must haveTranslationFor("confirmationPage.detail.1")
      messages must haveTranslationFor("confirmationPage.detail.2")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.title")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.detail")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.link")
      messages must haveTranslationFor("confirmationPage.whatHappensNext.link.text")
      messages must haveTranslationFor("confirmationPage.exitSurvey.link")
      messages must haveTranslationFor("confirmationPage.exitSurvey.link.text")
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

    "display panel" when {

      "no 'referenceId' has been provided" in {
        view.getElementsByClass(Styles.gdsPanelTitle).get(0) must containMessage(
          "confirmationPage.title"
        )
        view.getElementsByClass(Styles.gdsPanelBody).get(0) must containMessage(
          "confirmationPage.panel.body.default"
        )
      }

      "a 'referenceId' has been provided" in {
        val viewWithReferenceId = createView(flash = Flash(Map(FlashKeys.referenceId -> "PPT123")))
        viewWithReferenceId.getElementsByClass(Styles.gdsPanelTitle).get(0) must containMessage(
          "confirmationPage.title"
        )
        viewWithReferenceId.getElementsByClass(Styles.gdsPanelBody).get(0) must containMessage(
          "confirmationPage.panel.body",
          "PPT123"
        )
      }
    }

    "display body" in {

      val mainDetail = view.getElementsByClass(Styles.gdsPageBodyText)
      mainDetail.get(0) must containMessage("confirmationPage.detail.1")
      mainDetail.get(1) must containMessage("confirmationPage.detail.2")
    }

    "display 'What happens next'" in {

      view.getElementsByClass(Styles.gdsPageSubHeading).get(0) must containMessage(
        "confirmationPage.whatHappensNext.title"
      )

      val whatHappensNextDetail = view.getElementsByClass(Styles.gdsPageBodyText)
      whatHappensNextDetail.get(2) must containMessage("confirmationPage.whatHappensNext.detail")
      whatHappensNextDetail.get(3) must containMessage(
        "confirmationPage.whatHappensNext.link",
        messages("confirmationPage.whatHappensNext.link.text")
      )
      whatHappensNextDetail.get(4) must containMessage(
        "confirmationPage.exitSurvey.link",
        messages("confirmationPage.exitSurvey.link.text")
      )
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages, new Flash(Map.empty))
    page.render(request, messages, new Flash(Map.empty))
  }

}
