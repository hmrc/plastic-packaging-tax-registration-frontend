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
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContent, Call}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.liability.{
  routes => liabilityRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration
import uk.gov.hmrc.plasticpackagingtax.registration.models.request.JourneyRequest
import uk.gov.hmrc.plasticpackagingtax.registration.views.components.Styles
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.liability.check_liability_details_answers_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class CheckLiabilityDetailsAnswersViewSpec extends UnitViewSpec with Matchers {

  private val backLink = Call("GET", "backLink")

  private val page = inject[check_liability_details_answers_page]

  private val registration = aRegistration()

  "Check liability details answers View" should {
    val view = createView(reg = registration,
                          backLink =
                            liabilityRoutes.RegistrationTypeController.displayPage()
    )(journeyRequest)

    "have the correct title" in {
      view.select("title").first must containMessage("liability.checkAnswers.title")
    }

    "contain timeout dialog function" in {
      containTimeoutDialogFunction(view) mustBe true
    }

    "display sign out link" in {
      displaySignOutLink(view)
    }

    "display 'Back' button" when {
      "post-launch" in {
        view.getElementById("back-link") must haveHref(
          liabilityRoutes.RegistrationTypeController.displayPage()
        )
      }
    }

    "display title" in {
      view.getElementsByClass(Styles.gdsPageHeading).first() must containMessage(
        "liability.checkAnswers.title"
      )
    }

    "display expected content" when {

      "post-launch" in {
        assertSummaryRows(view,
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
                              "05 March 2022",
                              Some(
                                liabilityRoutes.ExpectToExceedThresholdWeightDateController.displayPage()
                              )
                            ),
                            SummaryRowDetail("liability.checkAnswers.startDate",
                                             "01 April 2022",
                                             None
                            ),
                            SummaryRowDetail(
                              "liability.checkAnswers.expectedWeightNext12m",
                              "12000 kg",
                              Some(liabilityRoutes.LiabilityWeightController.displayPage())
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

    "display 'Continue' button" in {
      view must containElementWithID("submit")
      view.getElementById("submit").text() mustBe "Continue"
    }

  }

  override def exerciseGeneratedRenderingMethods() = {
    page.f(registration, backLink)(journeyRequest, messages)
    page.render(registration, backLink, journeyRequest, messages)
  }

  private def createView(reg: Registration, backLink: Call)(implicit
    request: JourneyRequest[AnyContent]
  ): Document =
    page(reg, backLink)(request, messages(request))

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
