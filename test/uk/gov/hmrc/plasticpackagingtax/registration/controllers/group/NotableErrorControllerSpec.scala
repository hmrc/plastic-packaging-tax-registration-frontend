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

package uk.gov.hmrc.plasticpackagingtax.registration.controllers.group

import base.unit.ControllerSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.test.Helpers.{contentAsString, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.group.nominated_organisation_already_registered_page
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

class NotableErrorControllerSpec extends ControllerSpec {

  private val mcc = stubMessagesControllerComponents()

  private val nominatedOrganisationAlreadyRegisteredPage =
    mock[nominated_organisation_already_registered_page]

  private val controller =
    new NotableErrorController(authenticate = mockAuthAction,
                               mockJourneyAction,
                               mcc = mcc,
                               nominatedOrganisationAlreadyRegisteredPage =
                                 nominatedOrganisationAlreadyRegisteredPage
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    when(nominatedOrganisationAlreadyRegisteredPage.apply()(any(), any())).thenReturn(
      HtmlFormat.raw("error nominated organisation already been registered")
    )
  }

  "NotableErrorController" should {

    "present nominated organisation already been registered page" in {
      authorizedUser()
      val resp = controller.nominatedOrganisationAlreadyRegistered()(getRequest())

      status(resp) mustBe OK
      contentAsString(resp) mustBe "error nominated organisation already been registered"
    }
  }
}
