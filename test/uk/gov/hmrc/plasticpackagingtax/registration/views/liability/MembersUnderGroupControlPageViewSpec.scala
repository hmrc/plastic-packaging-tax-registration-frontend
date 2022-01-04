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

package uk.gov.hmrc.plasticpackagingtax.registration.views.liability

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.MembersUnderGroupControl
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.members_under_group_control_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class MembersUnderGroupControlPageViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[members_under_group_control_page]

  private def createView(
    form: Form[MembersUnderGroupControl] = MembersUnderGroupControl.form()
  ): Document =
    page(form)(journeyRequest, messages)

  "Members under group control view" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {

      displaySignOutLink(view)
    }

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(
        liabilityRoutes.RegistrationTypeController.displayPage()
      )
    }

    "display title" in {

      view.select("title").text() must include(messages("group.membersUnderGroupControl.title"))
    }

    "display radio inputs" in {

      view must containElementWithID("value")
      view.getElementsByClass("govuk-label").first().text() mustBe "Yes"
      view must containElementWithID("value-2")
      view.getElementsByClass("govuk-label").get(1).text() mustBe "No"
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods(): Unit = {
    page.f(MembersUnderGroupControl.form())(request, messages)
    page.render(MembersUnderGroupControl.form(), request, messages)
  }

}
