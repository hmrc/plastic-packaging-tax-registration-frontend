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

package uk.gov.hmrc.plasticpackagingtax.registration.views.enrolment

import base.unit.UnitViewSpec
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles._
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.enrolment.confirmation_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class ConfirmationViewSpec extends UnitViewSpec with Matchers {

  private val page: confirmation_page = instanceOf[confirmation_page]

  private def createView(): Html =
    page()(journeyRequest, messages)

  "Confirmation Page view" should {

    val view: Html = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("enrolment.confirmation.title"))
    }

    "display panel" in {
      view.getElementsByClass(gdsPanelTitle).get(0) must containMessage(
        "enrolment.confirmation.title"
      )

    }

    "display 'What happens next'" in {

      view.getElementsByClass(gdsPageSubHeading).get(0) must containMessage(
        "enrolment.confirmation.whatHappensNext.title"
      )

      val whatHappensNextDetail = view.getElementsByClass(gdsPageBodyText)
      whatHappensNextDetail.get(0) must containMessage(
        "enrolment.confirmation.whatHappensNext.detail.1",
        "PPT account"
      )
      whatHappensNextDetail.get(1) must containMessage(
        "enrolment.confirmation.whatHappensNext.detail.2",
        "BTA account"
      )
      whatHappensNextDetail.get(2) must containMessage(
        "enrolment.confirmation.whatHappensNext.detail.3",
        "What did you think of this service?"
      )
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f()(request, messages)
    page.render(request, messages)
  }

}
