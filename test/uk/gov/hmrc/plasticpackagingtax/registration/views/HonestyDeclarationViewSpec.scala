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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import spec.UnitViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.honesty_declaration
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class HonestyDeclarationViewSpec extends UnitViewSpec with Matchers {

  private val page                   = instanceOf[honesty_declaration]
  private def createView(): Document = page()(request, messages)

  "Honesty Declaration View" should {

    "have proper messages for labels" in {
      messages must haveTranslationFor("honestyDeclaration.title")
      messages must haveTranslationFor("honestyDeclaration.description")
      messages must haveTranslationFor("site.button.continue")
    }

    val view = createView()

    "display 'Back' button" in {

      view.getElementById("back-link") must haveHref(routes.RegistrationController.displayPage())
    }

    "display title" in {

      view.getElementsByClass("govuk-heading-xl").first() must containMessage(
        "honestyDeclaration.title"
      )
    }

    "display page hint" in {

      view.getElementsByClass("govuk-body").first() must containMessage(
        "honestyDeclaration.description"
      )
    }

    "display 'Continue' button" in {

      view must containElementWithID("submit")
      view.getElementById("submit") must containMessage("Continue")
    }
  }
}
