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
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import config.AppConfig
import play.api.i18n.{Lang, Messages, MessagesImpl}
import play.api.i18n.MessagesApi
import play.api.mvc.Flash
import play.twirl.api.Html
import views.html.confirmation_page
import views.html.deregistration.deregistration_submitted_page
import views.html.enrolment.{confirmation_page => enrolmentConfirmationPage}

class UserResearchBannerSpec extends UnitViewSpec with Matchers {

  private def appWith(bannerEnabled: Boolean): Application =
    new GuiceApplicationBuilder()
      .configure(
        "features.user-research-banner" -> bannerEnabled,
        "metrics.jvm"                   -> false,
        "metrics.enabled"               -> false
      )
      .build()

  val appConfig: AppConfig     = inject[AppConfig]
  val messagesApi: MessagesApi = inject[MessagesApi]

  private val confirmationPage = inject[confirmation_page]

  private lazy val bannerOn  = appWith(bannerEnabled = true)
  private lazy val bannerOff = appWith(bannerEnabled = false)

  private def welshMessages: Messages =
    MessagesImpl(Lang("cy"), messagesApi)

  private def optedInPages(app: Application, msgs: Messages): Seq[(String, Html)] = {
    val injector = app.injector
    Seq(
      "confirmation_page" -> injector.instanceOf[confirmation_page].apply()(
        registrationRequest,
        msgs,
        new Flash(Map.empty)
      ),
      "deregistration_submitted_page" -> injector.instanceOf[deregistration_submitted_page].apply()(
        registrationRequest,
        msgs
      ),
      "account_created_page" -> injector.instanceOf[enrolmentConfirmationPage].apply()(registrationRequest, msgs)
    )
  }

  private def asElement(html: Html): Element = Jsoup.parse(html.toString()).body()

  "The user research banner" should {

    "be displayed on every opted-in page in English" in {

      implicit val msgs: Messages = MessagesImpl(Lang("en"), messagesApi)

      optedInPages(bannerOn, msgs).foreach { case (name, html) =>
        withClue(s"$name: ")(containUserResearchBannerEnglish(asElement(html)))
      }
    }

    "be displayed on every opted-in page in Welsh" in {
      implicit val msgs: Messages = welshMessages
      optedInPages(bannerOn, msgs).foreach { case (name, html) =>
        withClue(s"$name: ")(containUserResearchBannerWelsh(asElement(html)))
      }
    }

    "not be displayed when the feature switch is disabled" in {
      val disabledMessages = MessagesImpl(Lang("en"), messagesApi)
      optedInPages(bannerOff, disabledMessages).foreach { case (name, html) =>
        withClue(s"$name: ")(asElement(html).select(".hmrc-user-research-banner").size() mustBe 0)
      }
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    confirmationPage.f()(registrationRequest, messages, new Flash(Map.empty))
    confirmationPage.render(registrationRequest, messages, new Flash(Map.empty))
  }
}
