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

package uk.gov.hmrc.plasticpackagingtax.registration.views.components

import base.unit.UnitViewSpec
import com.codahale.metrics.SharedMetricRegistries
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.components.saveButtons

abstract class SaveButtonsSpecBase extends UnitViewSpec with Matchers {

  protected val component: saveButtons = app.injector.instanceOf[saveButtons]

  override def exerciseGeneratedRenderingMethods(): Unit = {
    component.f("site.button.saveAndContinue", "site.button.saveAndComeBackLater")
    component.render("site.button.saveAndContinue", "site.button.saveAndComeBackLater", messages)
  }

}

class SaveButtonsSpec extends SaveButtonsSpecBase {

  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
      .configure("features.ukCompanyPrivateBeta" -> false)
      .build()
  }

  "Save Buttons Component" should {
    val view = component()(messages)

    "render both buttons" when {
      "outside of private BETA" in {
        val buttons = view.select("button")
        buttons.size() mustBe 2
        buttons.get(0).text() mustBe messages("site.button.saveAndContinue")
        buttons.get(1).text() mustBe messages("site.button.saveAndComeBackLater")
      }
    }
  }
}

class SaveButtonsPrivateBetaSpec extends SaveButtonsSpecBase with Matchers {

  override def fakeApplication(): Application = {
    SharedMetricRegistries.clear()
    new GuiceApplicationBuilder()
      .configure("features.ukCompanyPrivateBeta" -> true)
      .build()
  }

  "Save Buttons Component" should {
    val view = component()(messages)

    "render only the save button" when {
      "during private BETA" in {
        val buttons = view.select("button")
        buttons.size() mustBe 1
        buttons.get(0).text() mustBe messages("site.button.saveAndContinue")
      }
    }
  }
}
