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

package uk.gov.hmrc.plasticpackagingtax.registration.views.organisation

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{
  routes => organisationRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipName
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnershipName.form
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.organisation.partnership_name
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class PartnershipNameViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[partnership_name]

  private def createView(form: Form[PartnershipName] = PartnershipName.form()): Document =
    page(form)(journeyRequest, messages)

  "Partnership Name View" should {

    val view = createView()

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" in {
      view.getElementById("back-link") must haveHref(
        organisationRoutes.PartnershipTypeController.displayPage()
      )
    }

    "display title" in {
      view.select("title").text() must include(messages("partnership.name.title"))
    }

    "display page header" in {
      view.select("h1").text() must include(messages("partnership.name.title"))
    }

    "display text input field" in {
      view must containElementWithID("value")
    }

    "display 'Save and continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(form())(request, messages)
    page.render(form(), request, messages)
  }

}
