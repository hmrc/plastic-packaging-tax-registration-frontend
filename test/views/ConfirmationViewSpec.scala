/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.mvc.Flash
import play.api.test.Injecting
import play.twirl.api.Html
import config.AppConfig
import models.response.FlashKeys
import views.components.Styles._
import views.html.confirmation_page
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConfirmationViewSpec extends UnitViewSpec with Matchers with Injecting {

  private val realAppConfig           = inject[AppConfig]
  private val page: confirmation_page = inject[confirmation_page]

  private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  private def createView(flash: Flash = new Flash(Map.empty)): Html =
    page()(registrationRequest, messages, flash)

  "Confirmation Page view" should {

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
      "single entity registration" when {
        "no 'referenceId' has been provided" in {
          verifyPanelContent(
            view,
            messages("confirmationPage.heading", LocalDate.now.format(dateFormatter)),
            messages("confirmationPage.panel.body.default")
          )
        }
        "a 'referenceId' has been provided" in {
          val viewWithReferenceId =
            createView(flash = Flash(Map(FlashKeys.referenceId -> "PPT123", FlashKeys.groupReg -> false.toString)))
          verifyPanelContent(
            viewWithReferenceId,
            messages("confirmationPage.heading", LocalDate.now.format(dateFormatter)),
            messages("confirmationPage.panel.body", "PPT123")
          )
        }
      }
      "group registration" when {
        "no 'referenceId' has been provided" in {
          val groupView = createView(flash = Flash(Map(FlashKeys.groupReg -> true.toString)))
          verifyPanelContent(
            groupView,
            messages("confirmationPage.group.heading", LocalDate.now.format(dateFormatter)),
            messages("confirmationPage.panel.body.default")
          )
        }
        "a 'referenceId' has been provided" in {
          val groupViewWithReferenceId =
            createView(flash = Flash(Map(FlashKeys.referenceId -> "PPT123", FlashKeys.groupReg -> true.toString)))
          verifyPanelContent(
            groupViewWithReferenceId,
            messages("confirmationPage.group.heading", LocalDate.now.format(dateFormatter)),
            messages("confirmationPage.panel.body", "PPT123")
          )
        }
      }
    }

    "display main body" in {
      val mainDetail = createView().select("div#detail p")
      mainDetail.get(0) must containMessage("confirmationPage.detail.1")
      mainDetail.get(1) must containMessage("confirmationPage.detail.2")
      mainDetail.get(2).text must include(
        messages("confirmationPage.detail.3", messages("confirmationPage.detail.3.link"))
      )

      mainDetail.select("a").get(0) must haveHref(realAppConfig.pptAccountUrl)
    }

    "display 'What happens next'" when {

      "post-launch" in {
        view.getElementsByClass(gdsPageSubHeading).get(0) must containMessage("confirmationPage.whatHappensNext.title")

        val whatHappensNextDetail = view.select("div#what-happens-next p")
        whatHappensNextDetail.get(0).text must include(
          messages("confirmationPage.whatHappensNext.detail", messages("confirmationPage.whatHappensNext.detail.link"))
        )
        whatHappensNextDetail.select("a").get(0) must haveHref(realAppConfig.pptAccountUrl)

        val whatHappensNextDetailList = view.select("div#what-happens-next li")
        whatHappensNextDetailList.get(0).text must include(messages("confirmationPage.whatHappensNext.detail.item1"))
        whatHappensNextDetailList.get(1).text must include(messages("confirmationPage.whatHappensNext.detail.item2"))
      }
    }

    "display BTA info and link" in {
      val btaDetail = view.select("div#bta p")
      btaDetail.get(0).text must include(
        messages("confirmationPage.whatHappensNext.bta", messages("confirmationPage.whatHappensNext.bta.link"))
      )

      btaDetail.select("a").get(0) must haveHref(realAppConfig.businessAccountUrl)
    }

    "display exit survey link" in {
      val exitSurveyDetail = view.select("div#exit-survey p")

      exitSurveyDetail.get(0).text must include(messages("common.feedback.info"))
      exitSurveyDetail.get(1).text must include(
        messages("common.feedback.link", messages("common.feedback.link.description"))
      )

      exitSurveyDetail.select("a").get(0) must haveHref(realAppConfig.exitSurveyUrl)
    }
  }

  private def verifyPanelContent(view: Html, panelTitle: String, panelContent: String) = {
    view.getElementsByClass(gdsPanelTitle).get(0).text() must be(panelTitle)
    view.getElementsByClass(gdsPanelBody).get(0).text() must include(panelContent)
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(registrationRequest, messages, new Flash(Map.empty))
    page.render(registrationRequest, messages, new Flash(Map.empty))
  }

}
