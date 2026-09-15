/*
 * Copyright 2026 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import config.AppConfig
import play.api.i18n.{Lang, Messages, MessagesImpl}
import play.api.i18n.MessagesApi
import play.api.mvc.Flash
import play.twirl.api.Html
import views.html.confirmation_page
import views.html.deregistration.deregistration_submitted_page
import views.html.enrolment.{confirmation_page => enrolmentConfirmationPage}

class UserResearchBannerSpec extends UnitViewSpec with Matchers {

  val appConfig: AppConfig     = inject[AppConfig]
  val messagesApi: MessagesApi = inject[MessagesApi]

  private val confirmationPage   = inject[confirmation_page]
  private val deregistrationPage = inject[deregistration_submitted_page]
  private val accountCreatedPage = inject[enrolmentConfirmationPage]

  private def welshMessages: Messages =
    MessagesImpl(Lang("cy"), messagesApi)

  private def optedInPages(msgs: Messages): Seq[(String, Html)] = Seq(
    "confirmation_page"             -> confirmationPage()(registrationRequest, msgs, new Flash(Map.empty)),
    "deregistration_submitted_page" -> deregistrationPage()(registrationRequest, msgs),
    "account_created_page"          -> accountCreatedPage()(registrationRequest, msgs)
  )

  private def asElement(html: Html): Element = Jsoup.parse(html.toString()).body()

  "The user research banner" should {

    "be displayed on every opted-in page in English" in {
      optedInPages(messages).foreach { case (name, html) =>
        withClue(s"$name: ")(containUserResearchBannerEnglish(asElement(html)))
      }
    }

    "be displayed on every opted-in page in Welsh" in {
      optedInPages(welshMessages).foreach { case (name, html) =>
        withClue(s"$name: ")(containUserResearchBannerWelsh(asElement(html)))
      }
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    confirmationPage.f()(registrationRequest, messages, new Flash(Map.empty))
    confirmationPage.render(registrationRequest, messages, new Flash(Map.empty))
  }
}
