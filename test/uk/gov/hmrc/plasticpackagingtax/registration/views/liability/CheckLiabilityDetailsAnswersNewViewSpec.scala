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
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.{
  check_liability_details_answers_new_page,
  check_liability_details_answers_page
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

import java.time.format.DateTimeFormatter

@ViewTest
class CheckLiabilityDetailsAnswersNewViewSpec extends UnitViewSpec with Matchers {

  private val mockAppConfig = mock[AppConfig]

  private val backLink = Call("GET", "backLink")

  private val page = inject[check_liability_details_answers_new_page]

  private val populatedRegistration   = aRegistration()
  private val unpopulatedRegistration = Registration("id")

  "Check liability details answers View" should {
    val preLaunchView = createView(preLaunch = true,
                                   reg = populatedRegistration,
                                   backLink = routes.TaskListController.displayPage()
    )
    val preLaunchViewWithEmptyRegistration =
      createView(preLaunch = true,
                 reg = unpopulatedRegistration,
                 backLink = routes.TaskListController.displayPage()
      )

    val postLaunchView = createView(preLaunch = false,
                                    reg = populatedRegistration,
                                    backLink =
                                      liabilityRoutes.LiabilityStartDateController.displayPage()
    )(generateRequest(userFeatureFlags = Map(Features.isPreLaunch -> false)))

    val postLaunchViewWithEmptyRegistration =
      createView(preLaunch = false,
                 reg = unpopulatedRegistration,
                 backLink = liabilityRoutes.LiabilityStartDateController.displayPage()
      )(generateRequest(userFeatureFlags = Map(Features.isPreLaunch -> false)))

    "have the correct title" in {
      List(preLaunchView, postLaunchView).foreach { view =>
        view.select("title").first must containMessage("checkLiabilityDetailsAnswers.title")
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
      "feature flag 'isPreLaunch' is enabled" in {
        preLaunchView.getElementById("back-link") must haveHref(
          routes.TaskListController.displayPage()
        )
      }

      "feature flag 'isPreLaunch' is not enabled" in {
        postLaunchView.getElementById("back-link") must haveHref(
          liabilityRoutes.LiabilityStartDateController.displayPage()
        )
      }
    }

    "display title" in {
      List(preLaunchView, postLaunchView).foreach { view =>
        view.getElementsByClass(Styles.gdsPageHeading).first() must containMessage(
          "checkLiabilityDetailsAnswers.title"
        )
      }
    }

    "display expected content" when {
      val populatedWeightRow =
        SummaryRowDetail("checkLiabilityDetailsAnswers.weight",
                         populatedRegistration.liabilityDetails.weight.get.totalKg.get.toString,
                         liabilityRoutes.LiabilityWeightController.displayPage()
        )
      val unpopulatedWeightRow = populatedWeightRow.copy(value = "")

      "'preLaunch' feature flag enabled" when {
        val populatedExceedIn30Row =
          SummaryRowDetail(
            "checkLiabilityDetailsAnswers.future.exceed",
            messages("general.true"),
            liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage()
          )

        val populatedIsLiableRow =
          SummaryRowDetail("checkLiabilityDetailsAnswers.future.liable",
                           messages("general.true"),
                           liabilityRoutes.LiabilityStartDateController.displayPage()
          )

        "registration populated" in {
          assertSummaryRows(preLaunchView,
                            List(populatedWeightRow, populatedExceedIn30Row, populatedIsLiableRow)
          )
        }

        "registration unpopulated" in {
          val unpopulatedIsLiableRow = populatedIsLiableRow.copy(value = "")

          assertSummaryRows(preLaunchViewWithEmptyRegistration,
                            List(unpopulatedWeightRow, unpopulatedIsLiableRow)
          )
        }
      }

      "'preLaunch' feature flag not enabled" when {
        val populatedLiableDateRow = SummaryRowDetail("checkLiabilityDetailsAnswers.date",
                                                      populatedRegistration.liabilityDetails.startDate.get.asLocalDate.format(
                                                        DateTimeFormatter.ofPattern("dd MMM yyyy")
                                                      ),
                                                      liabilityRoutes.LiabilityStartDateController.displayPage()
        )

        val populatedExceedIn30Row =
          SummaryRowDetail(
            "checkLiabilityDetailsAnswers.future.exceed",
            messages("general.true"),
            liabilityRoutes.LiabilityExpectToExceedThresholdWeightController.displayPage()
          )

        "registration populated" in {
          assertSummaryRows(postLaunchView,
                            List(populatedWeightRow, populatedExceedIn30Row, populatedLiableDateRow)
          )
        }

        "registration unpopulated" in {
          val unpopulatedLiableDateRow = populatedLiableDateRow.copy(value = "")

          assertSummaryRows(postLaunchViewWithEmptyRegistration,
                            List(unpopulatedWeightRow, unpopulatedLiableDateRow)
          )
        }
      }
    }

    "display 'Save and continue' button" in {
      preLaunchView must containElementWithID("submit")
      preLaunchView.getElementById("submit").text() mustBe "Save and continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(populatedRegistration, backLink, changeWeightLink)(journeyRequest, messages)
    page.render(populatedRegistration, backLink, changeWeightLink, journeyRequest, messages)
  }

  private def createView(preLaunch: Boolean, reg: Registration, backLink: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Document = {
    when(mockAppConfig.isDefaultFeatureFlagEnabled(refEq(Features.isPreLaunch))).thenReturn(
      preLaunch
    )
    page(reg, backLink, changeWeightLink)(request, messages(request))
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
      row._2 must haveHref(row._1.actionLink)
    }
  }

  private case class SummaryRowDetail(label: String, value: String, actionLink: Call)

}
