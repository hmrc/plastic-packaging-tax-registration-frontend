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

package uk.gov.hmrc.plasticpackagingtax.registration.views.partials

import base.PptTestData
import base.unit.UnitViewSpec
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.AuthenticatedRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.partials.phaseBanner

class PhaseBannerSpec extends UnitViewSpec with Matchers {

  private val bannerPartial = instanceOf[phaseBanner]

  private val requestPath          = "/plastic-packaging-tax/some-page"
  private implicit val fakeRequest = FakeRequest("GET", requestPath)

  private val authenticatedLink =
    "http://localhost:9250/contact/beta-feedback?" +
      "service=plastic-packaging-tax&" +
      "backUrl=http://localhost:9250/plastic-packaging-tax/some-page"

  private val unauthenticatedLink =
    "http://localhost:9250/contact/beta-feedback-unauthenticated?" +
      "service=plastic-packaging-tax&" +
      "backUrl=http://localhost:9250/plastic-packaging-tax/some-page"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(appConfig)
    when(appConfig.authenticatedFeedbackUrl()).thenReturn(authenticatedLink)
    when(appConfig.unauthenticatedFeedbackUrl()).thenReturn(unauthenticatedLink)
  }

  private def createBanner(request: Request[AnyContent] = fakeRequest): Html =
    bannerPartial("")(request, messages)

  "phaseBanner" should {

    "display feedback link with correct href" when {

      "validate other rendering methods" in {
        bannerPartial.f("beta")(request, messages)
        bannerPartial.render("beta", request, messages)
      }

      "when user authenticated" in {
        import utils.FakeRequestCSRFSupport._

        val request = new AuthenticatedRequest(FakeRequest("GET", requestPath).withCSRFToken,
                                               PptTestData.newUser()
        )
        createBanner(request)
          .getElementsByClass("govuk-phase-banner__text").first()
          .getElementsByTag("a").first() must haveHref(s"$authenticatedLink")
      }

      "when user unauthenticated" in {
        createBanner()
          .getElementsByClass("govuk-phase-banner__text").first()
          .getElementsByTag("a").first() must haveHref(s"$unauthenticatedLink")
      }
    }
  }

}
