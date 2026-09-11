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
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Flash
import play.twirl.api.Html
import views.html.confirmation_page
import views.html.deregistration.deregistration_submitted_page
import views.html.enrolment.{confirmation_page => enrolmentConfirmationPage}

class UserResearchBannerSpec extends UnitViewSpec with Matchers {

  private def appWith(bannerEnabled: Boolean) =
    new GuiceApplicationBuilder()
      .configure(
        "create-internal-auth-token-on-start" -> false,
        "features.user-research-banner" -> bannerEnabled 
      )
      .build()

  private lazy val bannerOnApp  = appWith(bannerEnabled = true)
  private lazy val bannerOffApp = appWith(bannerEnabled = false)
  lazy val confirmationPage: confirmation_page = inject[confirmation_page]

  private def messagesIn(app: play.api.Application, lang: Lang): Messages =
    MessagesImpl(lang, app.injector.instanceOf[MessagesApi])

  private def renderConfirmation(app: play.api.Application, msgs: Messages): Html =
    confirmationPage(registrationRequest, msgs, new Flash(Map.empty))

  private def renderDeregistration(app: play.api.Application, msgs: Messages): Html =
    deregistration_submitted_page()(registrationRequest, msgs)

  private def renderAccountCreated(app: play.api.Application, msgs: Messages): Html =
    app.injector.instanceOf[enrolmentConfirmationPage].apply()(registrationRequest, msgs)

  private def optedInPages(app: play.api.Application, msgs: Messages): Seq[(String, Html)] = Seq(
    "confirmation_page"             -> renderConfirmation(app, msgs),
    "deregistration_submitted_page" -> renderDeregistration(app, msgs),
    "account_created_page"          -> renderAccountCreated(app, msgs)
  )

  private def asElement(html: Html): Element = Jsoup.parse(html.toString()).body()

  "The user research banner" should {

    "be displayed on every opted-in page in English" in {
      val msgs = messagesIn(bannerOnApp, Lang("en"))
      optedInPages(bannerOnApp, msgs).foreach { case (name, html) =>
        withClue(s"$name: ")(containUserResearchBannerEnglish(asElement(html)))
      }
    }

    "be displayed on every opted-in page in Welsh" in {
      val msgs = messagesIn(bannerOnApp, Lang("cy"))
      optedInPages(bannerOnApp, msgs).foreach { case (name, html) =>
        withClue(s"$name: ")(containUserResearchBannerWelsh(asElement(html)))
      }
    }

    "not be displayed on any opted-in page when the feature flag is off" in {
      val msgs = messagesIn(bannerOffApp, Lang("en"))
      optedInPages(bannerOffApp, msgs).foreach { case (name, html) =>
        withClue(s"$name: ")(containNoUserResearchBanner(asElement(html)))
      }
    }
  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    confirmationPage(registrationRequest, messages, new Flash(Map.empty))
    confirmationPage.render(registrationRequest, messages, new Flash(Map.empty))
  }
}

  