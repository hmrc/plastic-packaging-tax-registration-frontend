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

package base.unit

import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers.{include, mustBe}
import spec.ViewMatchers
import controllers.routes
import org.scalatest.matchers.should.Matchers.should
import views.viewmodels.SignOutReason

trait ViewAssertions extends ViewMatchers {

  def containTimeoutDialogFunction(view: Element) =
    view.select("meta[name='hmrc-timeout-dialog']").size() == 1

  def displaySignOutLink(view: Element) = {
    view.getElementsByClass("hmrc-sign-out-nav__link").first().text() should include("Sign out")
    view.getElementsByClass("hmrc-sign-out-nav__link").first() should haveHref(
      routes.SignOutController.signOut(SignOutReason.UserAction)
    )
  }

  def containUserResearchBannerEnglish(view: Element): Unit = {
    view.select(".hmrc-user-research-banner").size() mustBe 1

    val link = view.select(".hmrc-user-research-banner__link")
    link.attr("href") should include("https://banner-en")
  }

  def containUserResearchBannerWelsh(view: Element): Unit = {
    view.select(".hmrc-user-research-banner").size() mustBe 1

    val link = view.select(".hmrc-user-research-banner__link")
    link.attr("href") should include("https://banner-cy")
    link.text mustBe "Ymunwch â’n panel ymchwil (yn agor tab newydd)"
  }

  def containNoUserResearchBanner(view: Element): Unit =
    view.select(".hmrc-user-research-banner").size() mustBe 0
}
