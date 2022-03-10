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
import org.jsoup.nodes.{Document, Element}
import org.mockito.ArgumentMatchers.refEq
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContent, Call}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import uk.gov.hmrc.plasticpackagingtax.registration.config.{AppConfig, Features}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.prelaunch.{
  routes => prelaunchLiabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.check_liability_details_answers_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class CheckLiabilityDetailsAnswersViewSpec extends UnitViewSpec with Matchers {

  private val mockAppConfig = mock[AppConfig]

  private val backLink = Call("GET", "backLink")

  private val page = inject[check_liability_details_answers_page]

  private val registration = aRegistration()

  "Check liability details answers View" should {
    val preLaunchView = createView(preLaunch = true,
                                   reg = registration,
                                   backLink = routes.TaskListController.displayPage()
    )

    val postLaunchView = createView(preLaunch = false,
                                    reg = registration,
                                    backLink =
                                      liabilityRoutes.RegistrationTypeController.displayPage()
    )(generateRequest(userFeatureFlags = Map(Features.isPreLaunch -> false)))

    "have the correct title" in {
      List(preLaunchView, postLaunchView).foreach { view =>
        view.select("title").first must containMessage("liability.checkAnswers.title")
      }
    }

    "contain timeout dialog function" in {
      List(preLaunchView, postLaunchView).foreach { view =>
        containTimeoutDialogFunction(view) mustBe true
      }
    }

    "display sign out link" in {
      List(preLaunchView, postLaunchView).foreach { view =>
        displaySignOutLink(view)
      }
    }

    "display 'Back' button" when {
      "pre-launch" in {
        preLaunchView.getElementById("back-link") must haveHref(
          routes.TaskListController.displayPage()
        )
      }

      "post-launch" in {
        postLaunchView.getElementById("back-link") must haveHref(
          liabilityRoutes.RegistrationTypeController.displayPage()
        )
      }
    }

    "display title" in {
      List(preLaunchView, postLaunchView).foreach { view =>
        view.getElementsByClass(Styles.gdsPageHeading).first() must containMessage(
          "liability.checkAnswers.title"
        )
      }
    }

    "display expected content" when {
      "pre-launch" in {
        assertSummaryRows(preLaunchView,
                          List(
                            SummaryRowDetail(
                              "liability.checkAnswers.weight",
                              "12000 kg",
                              Some(
                                prelaunchLiabilityRoutes.LiabilityWeightExpectedController.displayPage()
                              )
                            ),
                            SummaryRowDetail(
                              "liability.checkAnswers.registrationType",
                              "A single organisation",
                              Some(liabilityRoutes.RegistrationTypeController.displayPage())
                            )
                          )
        )
      }

      "post-launch" in {
        assertSummaryRows(postLaunchView,
                          List(
                            SummaryRowDetail(
                              "liability.checkAnswers.exceededThreshold",
                              "No",
                              Some(liabilityRoutes.ExceededThresholdWeightController.displayPage())
                            ),
                            SummaryRowDetail(
                              "liability.checkAnswers.expectToExceededThreshold",
                              "Yes",
                              Some(
                                liabilityRoutes.ExpectToExceedThresholdWeightController.displayPage()
                              )
                            ),
                            SummaryRowDetail(
                              "liability.checkAnswers.dateRealisedExpectToExceededThreshold",
                              "05 Mar 2022",
                              Some(
                                liabilityRoutes.ExpectToExceedThresholdWeightDateController.displayPage()
                              )
                            ),
                            SummaryRowDetail("liability.checkAnswers.startDate",
                                             "01 Apr 2022",
                                             None
                            ),
                            SummaryRowDetail(
                              "liability.checkAnswers.registrationType",
                              "A single organisation",
                              Some(liabilityRoutes.RegistrationTypeController.displayPage())
                            )
                          )
        )
      }
    }

    "display 'Save and continue' button" in {
      preLaunchView must containElementWithID("submit")
      preLaunchView.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(registration, backLink)(journeyRequest, messages)
    page.render(registration, backLink, journeyRequest, messages)
  }

  private def createView(preLaunch: Boolean, reg: Registration, backLink: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Document = {
    when(mockAppConfig.isDefaultFeatureFlagEnabled(refEq(Features.isPreLaunch))).thenReturn(
      preLaunch
    )
    page(reg, backLink)(request, messages(request))
  }

  private def assertSummaryRows(view: Document, rows: List[SummaryRowDetail]) = {
    val summaryRowKeys = view.getElementsByClass("govuk-summary-list__key")
    rows.zip(summaryRowKeys).foreach { row =>
      row._2 must containMessage(row._1.label)
    }

    val summaryRowValues = view.getElementsByClass("govuk-summary-list__value")
    rows.zip(summaryRowValues).foreach { row =>
      row._2 must containText(row._1.value)
    }

    val summaryRowLinks = view.getElementsByClass("govuk-summary-list__value").filter {
      elem: Element => elem.hasClass("govuk-link")
    }
    rows.zip(summaryRowLinks).foreach { row =>
      if (row._1.actionLink.isDefined)
        row._2 must haveHref(row._1.actionLink.get)
    }
  }

  private case class SummaryRowDetail(label: String, value: String, actionLink: Option[Call])

}
