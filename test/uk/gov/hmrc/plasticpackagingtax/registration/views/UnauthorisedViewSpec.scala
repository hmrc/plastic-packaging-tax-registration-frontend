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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.unauthorised
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class UnauthorisedViewSpec extends UnitViewSpec with Matchers {

  private val page       = inject[unauthorised]
  override val appConfig = inject[AppConfig] // Use real AppConfig rather than inherited mock

  private def createView(): Document =
    page()(journeyRequest, messages)

  "Unauthorised Page view" should {

    val view = createView()

    "display page header" in {
      view.getElementsByTag("h1").first() must containMessage("unauthorised.heading")
    }

    "display required information" in {
      val contentParagraphs = view.select("p.govuk-body")

      contentParagraphs.get(0) must containMessage("unauthorised.paragraph.1")
      stripSpaceBeforeFullstop(contentParagraphs.get(1).text()) must include(
        messages("unauthorised.paragraph.2", messages("unauthorised.paragraph.2.link"))
      )
    }

    "display contact HMRC/deskpro link" in {
      val link = view.getElementById("deskpro_link")

      link must containMessage("unauthorised.paragraph.2.link")
      link must haveHref(appConfig.contactHmrcUrl)
      link.attributes().hasKey("target") mustBe false
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

  private def stripSpaceBeforeFullstop(text: String) = text.replaceAll(" \\.", ".")

}
