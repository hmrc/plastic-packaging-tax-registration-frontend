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

package registration.group

import support.BaseViewSpec
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.nominated_organisation_already_registered_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class NominatedOrganisationAlreadyRegisteredViewA11ySpec extends BaseViewSpec {

  private val page: nominated_organisation_already_registered_page =
    inject[nominated_organisation_already_registered_page]

  private def createView(): String = page()(journeyRequest, messages).toString()

  "Nominated Organisation Already Registered Page" should {

    val view = createView()

    "pass accessibility checks without error" in {
      view must passAccessibilityChecks
    }
  }
}
