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

package uk.gov.hmrc.plasticpackagingtax.registration.views.liability

import base.unit.UnitViewSpec
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.liability_weight_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class LiabilityWeightViewSpec extends UnitViewSpec with Matchers {

  private val page = instanceOf[liability_weight_page]

  private def createView(form: Form[LiabilityWeight] = LiabilityWeight.form()): Document =
    page(form)(journeyRequest, messages)

  "Liability Weight View" should {

    val view = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display 'Back' button" in {

      view.getElementById("back-link") mustBe null
    }

    "display title" in {

      view.select("title").text() must include(messages("liabilityWeightPage.title"))
    }

    "display header" in {

      view.getElementsByClass("govuk-caption-l").text() must include(
        messages("liabilityWeightPage.sectionHeader")
      )
    }

    "display liability weight question" in {

      view.getElementsByClass(Styles.gdsPageHeading).text() must include(
        messages("liabilityWeightPage.question")
      )
    }

    "display liability weight information" in {

      view.getElementsByClass("govuk-body").text() must include(
        messages("liabilityWeightPage.info",
                 "liabilityWeightPage.info2",
                 "liabilityWeightPage.guidance.description"
        )
      )
    }

    "display liability weight information link" in {

      val link = view.getElementById("guidance-link")
      link must haveHref(messages("liabilityWeightPage.guidance.href"))
      link.attr("target") mustBe "_blank"
      link.attr("rel") mustBe "noopener noreferrer"
    }

    "display total weight label" in {

      view.getElementsByAttributeValue("for", "totalKg").get(0).text() mustBe messages(
        "liabilityWeightPage.label"
      )
    }

    "display total weight input box" in {

      view must containElementWithID("totalKg")
    }

    "display 'Save and continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  "Liability Weight View when filled" should {

    "display data in weight input" in {

      val form = LiabilityWeight
        .form()
        .fill(aRegistration().liabilityDetails.weight.get)
      val view = createView(form)

      view.getElementById("totalKg").attr("value") mustBe "1000"
    }
  }

  "display error" when {

    "weight is not entered" in {

      val form = LiabilityWeight
        .form()
        .fillAndValidate(LiabilityWeight(None))
      val view = createView(form)

      view must haveGovukGlobalErrorSummary

      view must haveGovukFieldError("totalKg", "Weight cannot be empty")
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(LiabilityWeight.form())(request, messages)
    page.render(LiabilityWeight.form(), request, messages)
  }

}