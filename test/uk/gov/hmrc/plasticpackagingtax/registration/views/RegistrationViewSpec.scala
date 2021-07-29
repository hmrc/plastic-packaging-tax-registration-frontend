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
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, LiabilityWeight}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  MetaData,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class RegistrationViewSpec extends UnitViewSpec with Matchers {

  private val CHECK_AND_SUBMIT                    = 0
  private val LIABILITY_DETAILS                   = 0
  private val BUSINESS_DETAILS                    = 1
  private val PRIMARY_CONTACT_DETAILS             = 2
  private val registrationPage: registration_page = instanceOf[registration_page]

  private def createView(registration: Registration = aRegistration()): Html =
    registrationPage(registration)

  "Registration Page view" should {

    "have proper messages for labels" in {

      messages must haveTranslationFor("registrationPage.title")
      messages must haveTranslationFor("registrationPage.apply")
      messages must haveTranslationFor("registrationPage.completedSections")
      messages must haveTranslationFor("registrationPage.companyInformation")
      messages must haveTranslationFor("registrationPage.primaryContactDetails")
      messages must haveTranslationFor("registrationPage.liabilityDetails")
      messages must haveTranslationFor("registrationPage.checkAndSubmit")
      messages must haveTranslationFor("task.status.notStarted")
      messages must haveTranslationFor("task.status.inProgress")
      messages must haveTranslationFor("task.status.completed")
    }

    val view: Html = createView()

    "contain timeout dialog function" in {

      containTimeoutDialogFunction(view) mustBe true

    }

    "display sign out link" in {

      displaySignOutLink(view)

    }

    "display title" in {

      view.select("title").text() must include(messages("registrationPage.title"))
    }

    "display header" in {

      view.getElementById("title") must containMessage("registrationPage.title")
    }

    "display sections" when {
      def header(el: Element): String = el.getElementsByTag("h2").get(0).text()
      def sectionName(el: Element, index: Int): String =
        el.getElementsByTag("li").get(index + 1)
          .getElementsByClass("app-task-list__task-name").get(0).text()
      def sectionStatus(el: Element, index: Int): String =
        el.getElementsByTag("li").get(index + 1)
          .getElementsByClass("govuk-tag").get(0).text()
      def sectionLinks(el: Element, index: Int): Elements =
        el.getElementsByTag("li").get(index + 1)
          .getElementsByClass("govuk-link")
      def sectionLink(el: Element, index: Int): Element =
        sectionLinks(el, index).get(0)

      "Liability Details 'In Progress'" when {

        val registration = aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(1000))), startDate = None)
          ),
          withIncorpJourneyId(None),
          withNoPrimaryContactDetails()
        )
        val view: Html = createView(registration)

        "application status should reflect the completed sections" in {
          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe "Application incomplete"
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections",
                                   registration.numberOfCompletedSections
          )
        }

        "'Prepare application'" in {
          val prepareApplicationElem = view.getElementsByTag("li").get(0)

          header(prepareApplicationElem) must include(
            messages("registrationPage.prepareApplication")
          )

          sectionName(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "registrationPage.liabilityDetails"
          )
          sectionStatus(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "task.status.inProgress"
          )
          sectionLink(prepareApplicationElem, LIABILITY_DETAILS) must haveHref(
            routes.LiabilityWeightController.displayPage()
          )

          sectionName(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "registrationPage.companyInformation"
          )
          sectionStatus(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "task.status.cannotStartYet"
          )
          sectionName(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "registrationPage.primaryContactDetails"
          )
          sectionStatus(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "task.status.cannotStartYet"
          )
        }

        "'Apply'" in {
          val applyElem = view.getElementsByTag("li").get(4)

          header(applyElem) must include(messages("registrationPage.apply"))

          sectionName(applyElem, CHECK_AND_SUBMIT) mustBe messages(
            "registrationPage.checkAndSubmit"
          )
          sectionStatus(applyElem, CHECK_AND_SUBMIT) mustBe messages("task.status.cannotStartYet")
          sectionLinks(applyElem, CHECK_AND_SUBMIT).size() mustBe 0
        }
      }

      "Organisation information and Primary Contact details not started" when {

        val registration = aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(1000))),
                             startDate = Some(Date(Some(1), Some(4), Some(2022)))
            )
          ),
          withIncorpJourneyId(None),
          withNoPrimaryContactDetails()
        )
        val view: Html = createView(registration)

        "application status should reflect the completed sections" in {
          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe "Application incomplete"
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections",
                                   registration.numberOfCompletedSections
          )
        }

        "'Prepare application'" in {
          val prepareApplicationElem = view.getElementsByTag("li").get(0)

          header(prepareApplicationElem) must include(
            messages("registrationPage.prepareApplication")
          )

          sectionName(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "registrationPage.liabilityDetails"
          )
          sectionStatus(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "task.status.completed"
          )
          sectionLink(prepareApplicationElem, LIABILITY_DETAILS) must haveHref(
            routes.LiabilityWeightController.displayPage()
          )

          sectionName(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "registrationPage.companyInformation"
          )
          sectionStatus(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "task.status.notStarted"
          )
          sectionLink(prepareApplicationElem, BUSINESS_DETAILS) must haveHref(
            routes.OrganisationDetailsConfirmOrgBasedInUkController.displayPage()
          )

          sectionName(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "registrationPage.primaryContactDetails"
          )
          sectionStatus(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "task.status.cannotStartYet"
          )
        }

        "'Apply'" in {
          val applyElem = view.getElementsByTag("li").get(4)

          header(applyElem) must include(messages("registrationPage.apply"))

          sectionName(applyElem, CHECK_AND_SUBMIT) mustBe messages(
            "registrationPage.checkAndSubmit"
          )
          sectionStatus(applyElem, CHECK_AND_SUBMIT) mustBe messages("task.status.cannotStartYet")
          sectionLinks(applyElem, CHECK_AND_SUBMIT).size() mustBe 0
        }
      }

      "All Sections completed" when {

        val completeRegistration =
          aRegistration(
            withMetaData(MetaData(registrationReviewed = true, registrationCompleted = true))
          )

        val view: Html =
          createView(completeRegistration)

        "application status should reflect the completed sections" in {

          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe "Application complete"
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections",
                                   completeRegistration.numberOfCompletedSections
          )
        }

        "'Prepare application'" in {
          val prepareApplicationElem = view.getElementsByTag("li").get(0)

          header(prepareApplicationElem) must include(
            messages("registrationPage.prepareApplication")
          )

          sectionName(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "registrationPage.liabilityDetails"
          )
          sectionStatus(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "task.status.completed"
          )
          sectionLink(prepareApplicationElem, LIABILITY_DETAILS) must haveHref(
            routes.LiabilityWeightController.displayPage()
          )

          sectionName(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "registrationPage.companyInformation"
          )
          sectionStatus(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "task.status.completed"
          )
          sectionLink(prepareApplicationElem, BUSINESS_DETAILS) must haveHref(
            routes.OrganisationDetailsConfirmOrgBasedInUkController.displayPage()
          )

          sectionName(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "registrationPage.primaryContactDetails"
          )
          sectionStatus(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "task.status.completed"
          )
          sectionLink(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) must haveHref(
            routes.ContactDetailsFullNameController.displayPage()
          )
        }

        "'Apply'" in {

          val applyElem = view.getElementsByTag("li").get(4)

          header(applyElem) must include(messages("registrationPage.apply"))

          sectionName(applyElem, CHECK_AND_SUBMIT) mustBe messages(
            "registrationPage.checkAndSubmit"
          )
          sectionStatus(applyElem, CHECK_AND_SUBMIT) mustBe messages("task.status.completed")
          sectionLink(applyElem, CHECK_AND_SUBMIT) must haveHref(
            routes.ReviewRegistrationController.displayPage()
          )
        }
      }

      "Check and Submit is 'In Progress'" in {
        val inProgressRegistration = aRegistration(
          withMetaData(MetaData(registrationReviewed = true, registrationCompleted = false))
        )
        val view: Html = createView(inProgressRegistration)

        val applyElem = view.getElementsByTag("li").get(4)

        header(applyElem) must include(messages("registrationPage.apply"))

        sectionName(applyElem, CHECK_AND_SUBMIT) mustBe messages("registrationPage.checkAndSubmit")
        sectionStatus(applyElem, CHECK_AND_SUBMIT) mustBe messages("task.status.inProgress")
        sectionLink(applyElem, CHECK_AND_SUBMIT) must haveHref(
          routes.ReviewRegistrationController.displayPage()
        )
      }

      "Check and Submit is 'Completed'" in {
        val completedRegistration = aRegistration(
          withMetaData(MetaData(registrationReviewed = true, registrationCompleted = true))
        )
        val view: Html = createView(completedRegistration)

        val applyElem = view.getElementsByTag("li").get(4)

        header(applyElem) must include(messages("registrationPage.apply"))

        sectionName(applyElem, CHECK_AND_SUBMIT) mustBe messages("registrationPage.checkAndSubmit")
        sectionStatus(applyElem, CHECK_AND_SUBMIT) mustBe messages("task.status.completed")
        sectionLink(applyElem, CHECK_AND_SUBMIT) must haveHref(
          routes.ReviewRegistrationController.displayPage()
        )
      }
    }
  }
}
