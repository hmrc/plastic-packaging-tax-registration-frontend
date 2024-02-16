/*
 * Copyright 2024 HM Revenue & Customs
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

package views.group

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import views.components.Styles.gdsPageBodyText
import views.html.group.nominated_organisation_already_registered_page

class NominatedOrganisationAlreadyRegisteredViewSpec extends UnitViewSpec with Matchers {

  private val page: nominated_organisation_already_registered_page =
    inject[nominated_organisation_already_registered_page]

  private def createView(): Html = page()(registrationJourneyRequest, messages)

  "Nominated Organisation Already Registered Page" should {

    val view: Html = createView()

    "display title" in {
      view.select("title").text() must include(messages("nominated.organisation.already.registered.title"))
    }

    "display heading" in {
      view.select("h1").text() must include(messages("nominated.organisation.already.registered.title"))
    }

    "display detail" in {
      view.select("p.govuk-body").text() must include(messages("nominated.organisation.already.registered.detail1", "Plastic Packaging Ltd"))

      val details = view.getElementsByClass(gdsPageBodyText)
      details.get(1) must containMessage("nominated.organisation.already.registered.detail2")
      details.get(2) must containMessage("nominated.organisation.already.registered.detail3", messages("nominated.organisation.already.registered.detail3.link.text"))
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(registrationJourneyRequest, messages)
    page.render(registrationJourneyRequest, messages)
  }

}
