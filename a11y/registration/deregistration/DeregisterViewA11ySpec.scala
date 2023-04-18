/*
 * Copyright 2023 HM Revenue & Customs
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

package registration.deregistration

import play.api.data.Form
import support.BaseViewSpec
import config.AppConfig
import forms.deregistration.DeregisterForm
import views.html.deregistration.deregister_page

class DeregisterViewA11ySpec extends BaseViewSpec {

  override val appConfig: AppConfig = inject[AppConfig]
  private val page                  = inject[deregister_page]

  private def createView(form: Form[Boolean] = DeregisterForm.form()): String =
    page(form)(registrationRequest, messages).toString()

  "The Initial Deregister View" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }

    "pass accessibility checks with error" in {
      val form = DeregisterForm
        .form()
        .bind(Map("value" -> ""))
      val view = createView(form)

      view must passAccessibilityChecks
    }
  }
}
