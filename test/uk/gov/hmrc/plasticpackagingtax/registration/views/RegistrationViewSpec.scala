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
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.model.TaskStatus
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class RegistrationViewSpec extends UnitViewSpec with Matchers {

  private val CHECK_AND_SUBMIT                    = 0
  private val BUSINESS_DETAILS                    = 0
  private val PRIMARY_CONTACT_DETAILS             = 1
  private val LIABILITY_DETAILS                   = 2
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

      "Company Information complete and Liability Details Incomplete" when {

        val registration = aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(1000))), startDate = None)
          ),
          withIncorpJourneyId(Some("123"))
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

          sectionName(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "registrationPage.companyInformation"
          )
          sectionStatus(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "task.status.completed"
          )
          sectionLink(prepareApplicationElem, BUSINESS_DETAILS) must haveHref(
            routes.HonestyDeclarationController.displayPage()
          )

          sectionName(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "registrationPage.primaryContactDetails"
          )
          sectionStatus(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "task.status.inProgress"
          )
          sectionLink(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) must haveHref(
            routes.ContactDetailsFullNameController.displayPage()
          )

          sectionName(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "registrationPage.liabilityDetails"
          )
          sectionStatus(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "task.status.inProgress"
          )
          sectionLink(prepareApplicationElem, LIABILITY_DETAILS) must haveHref(
            routes.LiabilityStartDateController.displayPage()
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

      "Liability Details not started" when {

        val registration = aRegistration(withNoLiabilityDetails(), withIncorpJourneyId(Some("123")))
        val view: Html   = createView(registration)

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

          sectionName(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "registrationPage.companyInformation"
          )
          sectionStatus(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "task.status.completed"
          )
          sectionLink(prepareApplicationElem, BUSINESS_DETAILS) must haveHref(
            routes.HonestyDeclarationController.displayPage()
          )

          sectionName(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "registrationPage.primaryContactDetails"
          )
          sectionStatus(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) mustBe messages(
            "task.status.inProgress"
          )
          sectionLink(prepareApplicationElem, PRIMARY_CONTACT_DETAILS) must haveHref(
            routes.ContactDetailsFullNameController.displayPage()
          )

          sectionName(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "registrationPage.liabilityDetails"
          )
          sectionStatus(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "task.status.notStarted"
          )
          sectionLink(prepareApplicationElem, LIABILITY_DETAILS) must haveHref(
            routes.LiabilityStartDateController.displayPage()
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

        val spyCompleteRegistration = Mockito.spy(aRegistration())
        when(spyCompleteRegistration.isPrimaryContactDetailsComplete).thenReturn(true)
        when(spyCompleteRegistration.primaryContactDetailsStatus).thenReturn(TaskStatus.Completed)
        when(spyCompleteRegistration.isCheckAndSubmitComplete).thenReturn(true)
        when(spyCompleteRegistration.checkAndSubmitStatus).thenReturn(TaskStatus.Completed)

        val view: Html =
          createView(spyCompleteRegistration)

        "application status should reflect the completed sections" in {
          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe "Application complete"
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections",
                                   spyCompleteRegistration.numberOfCompletedSections
          )
        }

        "'Prepare application'" in {
          val prepareApplicationElem = view.getElementsByTag("li").get(0)

          header(prepareApplicationElem) must include(
            messages("registrationPage.prepareApplication")
          )

          sectionName(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "registrationPage.companyInformation"
          )
          sectionStatus(prepareApplicationElem, BUSINESS_DETAILS) mustBe messages(
            "task.status.completed"
          )
          sectionLink(prepareApplicationElem, BUSINESS_DETAILS) must haveHref(
            routes.HonestyDeclarationController.displayPage()
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

          sectionName(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "registrationPage.liabilityDetails"
          )
          sectionStatus(prepareApplicationElem, LIABILITY_DETAILS) mustBe messages(
            "task.status.completed"
          )
          sectionLink(prepareApplicationElem, LIABILITY_DETAILS) must haveHref(
            routes.LiabilityStartDateController.displayPage()
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
            routes.RegistrationController.displayPage()
          )
        }
      }
    }
  }
}
