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

import base.unit.UnitViewSpec
import org.jsoup.nodes.{Document, Element}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukButton, GovukSummaryList}
import uk.gov.hmrc.govukfrontend.views.html.helpers.formWithCSRF
import uk.gov.hmrc.plasticpackagingtax.registration.config.AppConfig
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.components.{
  pageTitle,
  saveAndContinue
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.{
  check_liability_details_answers_page,
  main_template
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

import java.time.format.DateTimeFormatter

@ViewTest
class CheckLiabilityDetailsAnswersViewSpec extends UnitViewSpec with Matchers {

  private val formHelper       = instanceOf[formWithCSRF]
  private val govukLayout      = instanceOf[main_template]
  private val govukButton      = instanceOf[GovukButton]
  private val govukSummaryList = instanceOf[GovukSummaryList]
  private val pageTitle        = instanceOf[pageTitle]
  private val saveAndContinue  = instanceOf[saveAndContinue]
  private val mockAppConfig    = mock[AppConfig]

  private val page = new check_liability_details_answers_page(formHelper,
                                                              govukLayout,
                                                              govukButton,
                                                              govukSummaryList,
                                                              pageTitle,
                                                              saveAndContinue,
                                                              mockAppConfig
  )

  private val populatedRegistration   = aRegistration()
  private val unpopulatedRegistration = Registration("id")

  "Check liability details answers View" should {
    val preLaunchView = createView(preLaunch = true,
                                   reg = populatedRegistration,
                                   backLink = routes.LiabilityLiableDateController.displayPage()
    )
    val preLaunchViewWithEmptyRegistration =
      createView(preLaunch = true,
                 reg = unpopulatedRegistration,
                 backLink = routes.LiabilityLiableDateController.displayPage()
      )

    val postLaunchView = createView(preLaunch = false,
                                    reg = populatedRegistration,
                                    backLink = routes.LiabilityStartDateController.displayPage()
    )
    val postLaunchViewWithEmptyRegistration =
      createView(preLaunch = false,
                 reg = unpopulatedRegistration,
                 backLink = routes.LiabilityStartDateController.displayPage()
      )

    "have proper messages for labels" in {
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.title")
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.date")
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.weight")
      messages must haveTranslationFor("site.link.change")
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.future.exceed")
      messages must haveTranslationFor("checkLiabilityDetailsAnswers.future.liable")
      messages must haveTranslationFor("general.true")
      messages must haveTranslationFor("general.false")
    }

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
          routes.LiabilityLiableDateController.displayPage()
        )
      }

      "feature flag 'isPreLaunch' is not enabled" in {
        postLaunchView.getElementById("back-link") must haveHref(
          routes.LiabilityStartDateController.displayPage()
        )
      }
    }

    "display title" in {
      List(preLaunchView, postLaunchView).foreach { view =>
        view.getElementsByClass("govuk-heading-xl").first() must containMessage(
          "checkLiabilityDetailsAnswers.title"
        )
      }
    }

    "display expected content" when {
      val populatedWeightRow =
        SummaryRowDetail("checkLiabilityDetailsAnswers.weight",
                         populatedRegistration.liabilityDetails.weight.get.totalKg.get.toString,
                         routes.LiabilityWeightController.displayPage()
        )
      val unpopulatedWeightRow = populatedWeightRow.copy(value = "")

      "'preLaunch' feature flag enabled" when {
        val populatedExceedIn30Row =
          SummaryRowDetail("checkLiabilityDetailsAnswers.future.exceed",
                           messages("general.true"),
                           routes.LiabilityExpectToExceedThresholdWeightController.displayPage()
          )

        val populatedIsLiableRow =
          SummaryRowDetail("checkLiabilityDetailsAnswers.future.liable",
                           messages("general.true"),
                           routes.LiabilityStartDateController.displayPage()
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
                                                      routes.LiabilityStartDateController.displayPage()
        )

        "registration populated" in {
          assertSummaryRows(postLaunchView, List(populatedWeightRow, populatedLiableDateRow))
        }

        "registration unpopulated" in {
          val unpopulatedLiableDateRow = populatedLiableDateRow.copy(value = "")

          assertSummaryRows(postLaunchViewWithEmptyRegistration,
                            List(unpopulatedWeightRow, unpopulatedLiableDateRow)
          )
        }
      }
    }

    "display 'Save And Continue' button" in {
      preLaunchView must containElementWithID("submit")
      preLaunchView.getElementById("submit").text() mustBe "Save and Continue"
    }

    // TODO: we need a better way of achieving the minimum test code coverage than doing this!
    "validate other rendering methods" in {
      page.f(populatedRegistration, routes.LiabilityLiableDateController.displayPage())(request,
                                                                                        messages
      ).select("title").text() must include(messages("checkLiabilityDetailsAnswers.title"))
      page.render(registration = populatedRegistration,
                  backLink = routes.LiabilityLiableDateController.displayPage(),
                  request = request,
                  messages = messages
      ).select("title").text() must include(messages("checkLiabilityDetailsAnswers.title"))
    }

  }

  private def createView(preLaunch: Boolean, reg: Registration, backLink: Call): Document = {
    when(mockAppConfig.isPreLaunch).thenReturn(preLaunch)
    page(reg, backLink)(request, messages)
  }

  private case class SummaryRowDetail(label: String, value: String, actionLink: Call)

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

}
