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

package views

import base.unit.UnitViewSpec
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import play.twirl.api.Html
import controllers.contact.{routes => contactRoutes}
import controllers.organisation.{routes => organisationRoutes}
import forms.liability.LiabilityWeight
import forms.liability.RegType.GROUP
import forms.{Date, OldDate}
import models.registration._
import views.html.task_list_group

import java.time.LocalDate


class RegistrationGroupViewSpec extends UnitViewSpec with Matchers {

  private val LIABILITY_DETAILS = 0
  private val NOMINATED_BUSINESS_DETAILS = 1
  private val OTHER_BUSINESS_DETAILS = 2
  private val CHECK_AND_SUBMIT = 3
  private val registrationPage: task_list_group = inject[task_list_group]

  private val liabilityStartLink = Call("GET", "/liabilityStartLink")

  private def createView(registration: Registration = aRegistration()): Html =
    registrationPage(registration, liabilityStartLink, showRestartLiabilityContent = false)

  "Registration Group Page view" should {

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

      view.getElementsByTag("h1") must containMessageForElements("registrationPage.title")
    }

    "display sections" when {
      def header(el: Element): String = el.getElementsByTag("h2").get(0).text()

      def sectionName(el: Element, index: Int): String =
        el.getElementsByClass("app-task-list__task-name").get(index).text()

      def sectionStatus(el: Element, index: Int): String =
        el.getElementsByClass("govuk-tag").get(index).text()


      def sectionLink(el: Element, index: Int): Element =
        el.getElementsByClass("govuk-link").get(index)

      "Liability Details 'In Progress'" when {

        val registration = aRegistration(withRegistrationType(Some(GROUP)),
          withLiabilityDetails(
            LiabilityDetails(expectToExceedThresholdWeight = Some(true),
              newLiabilityStarted = Some(NewLiability))
          ),
          withIncorpJourneyId(None),
          withNoPrimaryContactDetails()
        )
        val view: Html = createView(registration)

        "application status should reflect the completed sections" in {
          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe messages("registrationPage.subheading.incomplete")
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections",
            registration.numberOfCompletedSections,
            5
          )
        }

        "Eligibility check" in {
          val liabilityElement = view.getElementsByClass("app-task").get(LIABILITY_DETAILS)

          header(liabilityElement) must include(
            messages("registrationPage.task.eligibility.heading")
          )
          sectionName(liabilityElement, 0) mustBe messages("registrationPage.task.eligibility")
          sectionStatus(liabilityElement, 0) mustBe messages("task.status.inProgress")
          sectionLink(liabilityElement, 0) must haveHref(liabilityStartLink)
        }

        "Nominated organisation details" in {//todo: should status logic be tested here?

          val businessElement = view.getElementsByClass("app-task").get(NOMINATED_BUSINESS_DETAILS)

          header(businessElement) must include(
            messages("registrationPage.task.organisation.heading.group")
          )

          sectionName(businessElement, 0) mustBe messages(
            "registrationPage.task.organisation.group"
          )
          sectionStatus(businessElement, 0) mustBe messages("task.status.completed")
        }

        "Nominated organisation contact details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(NOMINATED_BUSINESS_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.organisation.heading.group")
          )

          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.group"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.notStarted")
        }

        "Other organisations in group details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(OTHER_BUSINESS_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.otherOrganisation.heading")
          )

          sectionName(contactElement, 0) mustBe messages("registrationPage.task.otherOrganisation")
          sectionStatus(contactElement, 0) mustBe messages("task.status.cannotStartYet")
        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.cannotStartYet")
        }

      }

      "Organisation information and Primary Contact details not started" when {

        val registration = aRegistration(withRegistrationType(Some(GROUP)),
          withGroupDetail(
            Some(GroupDetail(membersUnderGroupControl = Some(true)))
          ),
          withLiabilityDetails(
            LiabilityDetails(exceededThresholdWeight = Some(true), //todo- check if this should pass
              dateExceededThresholdWeight = Some(
                Date(LocalDate.parse("2022-03-05")),
              ),
              expectedWeightNext12m =
                Some(LiabilityWeight(Some(12000))),
              startDate = Some(
                OldDate(Some(1), Some(4), Some(2022))
              ), newLiabilityStarted = Some(NewLiability),
              newLiabilityFinished = Some(NewLiability)
            )
          ),
          withOrganisationDetails(OrganisationDetails()),
          withNoPrimaryContactDetails()
        )
        val view: Html = createView(registration)

        "application status should reflect the completed sections" in {
          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe messages("registrationPage.subheading.incomplete")
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections",
            registration.numberOfCompletedSections,
            5
          )
        }

        "Eligibility check" in {
          val liabilityElement = view.getElementsByClass("app-task").get(LIABILITY_DETAILS)

          header(liabilityElement) must include(
            messages("registrationPage.task.eligibility.heading")
          )
          sectionName(liabilityElement, 0) mustBe messages("registrationPage.task.eligibility")
          sectionStatus(liabilityElement, 0) mustBe messages("task.status.completed")
          sectionLink(liabilityElement, 0) must haveHref(liabilityStartLink)
        }

        "Nominated organisation details" in {
          val organisationElement =
            view.getElementsByClass("app-task").get(NOMINATED_BUSINESS_DETAILS)

          header(organisationElement) must include(
            messages("registrationPage.task.organisation.heading.group")
          )

          sectionName(organisationElement, 0) mustBe messages(
            "registrationPage.task.organisation.group"
          )
          sectionStatus(organisationElement, 0) mustBe messages("task.status.notStarted")
          sectionLink(organisationElement, 0) must haveHref(
            organisationRoutes.OrganisationDetailsTypeController.submitRepresentativeMember()
          )
        }

        "Nominated organisation contact details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(NOMINATED_BUSINESS_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.organisation.heading.group")
          )

          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.group"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.cannotStartYet")
        }

        "Other organisations in group details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(OTHER_BUSINESS_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.otherOrganisation.heading")
          )

          sectionName(contactElement, 0) mustBe messages("registrationPage.task.otherOrganisation")
          sectionStatus(contactElement, 0) mustBe messages("task.status.cannotStartYet")
        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.cannotStartYet")
        }

      }

      "Primary contact email not verified" when {

        val registration =
          aRegistration(withRegistrationType(Some(GROUP)),
            withMetaData(MetaData()),
            withGroupDetail(Some(GroupDetail(membersUnderGroupControl = Some(true))))
          )

        val view: Html =
          createView(registration)

        "Nominated organisation contact details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(NOMINATED_BUSINESS_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.organisation.heading.group")
          )

          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.group"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.inProgress")
          sectionLink(contactElement, 1) must haveHref(
            contactRoutes.ContactDetailsFullNameController.displayPage()
          )
        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.cannotStartYet")
        }

      }

      "All Sections completed" when {

        val registrationCompletedMetaData =
          aRegistration().metaData.copy(registrationReviewed = true, registrationCompleted = true)
        val completeRegistration =
          aRegistration(withRegistrationType(Some(GROUP)),
            withMetaData(registrationCompletedMetaData),
            withGroupDetail(Some(GroupDetail(membersUnderGroupControl = Some(true))))
          )

        val view: Html =
          createView(completeRegistration)

        "application status should reflect the completed sections" in {

          // TODO - fix when other groups done
          //          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
          //            0
          //          ).text() mustBe messages("registrationPage.subheading.complete")
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections",
            completeRegistration.numberOfCompletedSections,
            5
          )
        }

        "Eligibility check" in {
          val liabilityElement = view.getElementsByClass("app-task").get(LIABILITY_DETAILS)

          header(liabilityElement) must include(
            messages("registrationPage.task.eligibility.heading")
          )
          sectionName(liabilityElement, 0) mustBe messages("registrationPage.task.eligibility")
          sectionStatus(liabilityElement, 0) mustBe messages("task.status.completed")
          sectionLink(liabilityElement, 0) must haveHref(liabilityStartLink)
        }

        "Nominated organisation details" in {
          val organisationElement =
            view.getElementsByClass("app-task").get(NOMINATED_BUSINESS_DETAILS)

          header(organisationElement) must include(
            messages("registrationPage.task.organisation.heading.group")
          )

          sectionName(organisationElement, 0) mustBe messages(
            "registrationPage.task.organisation.group"
          )
          sectionStatus(organisationElement, 0) mustBe messages("task.status.completed")
          sectionLink(organisationElement, 0) must haveHref(
            organisationRoutes.CheckAnswersController.displayPage()
          )
        }

        "Nominated organisation contact details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(NOMINATED_BUSINESS_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.organisation.heading.group")
          )

          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.group"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.completed")
          sectionLink(contactElement, 1) must haveHref(
            contactRoutes.ContactDetailsCheckAnswersController.displayPage()
          )
        }

        // TODO - Other organisations in group "complete"

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")

          // TODO - fix when other groups done
          //          sectionStatus(reviewElement, 0) mustBe messages("task.status.completed")
          //          sectionLink(reviewElement, 0) must haveHref(
          //            routes.ReviewRegistrationController.displayPage()
          //          )
        }

      }

      "Check and Submit is 'In Progress'" in {

        val inProgressMetaData =
          aRegistration().metaData.copy(registrationReviewed = true, registrationCompleted = false)
        val inProgressRegistration =
          aRegistration(withRegistrationType(Some(GROUP)), withMetaData(inProgressMetaData))
        val view: Html = createView(inProgressRegistration)

        val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

        header(reviewElement) must include(messages("registrationPage.task.review.heading"))

        sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")

        // TODO - fix when other groups done
        //        sectionStatus(reviewElement, 0) mustBe messages("task.status.inProgress")
        //        sectionLink(reviewElement, 0) must haveHref(
        //          routes.ReviewRegistrationController.displayPage()
        //        )
      }

      "Check and Submit is 'Completed'" in {

        val completedMetaData =
          aRegistration().metaData.copy(registrationReviewed = true, registrationCompleted = true)
        val completedRegistration =
          aRegistration(withRegistrationType(Some(GROUP)), withMetaData(completedMetaData))
        val view: Html = createView(completedRegistration)

        val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

        header(reviewElement) must include(messages("registrationPage.task.review.heading"))

        sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")

        // TODO - fix when other groups done
        //        sectionStatus(reviewElement, 0) mustBe messages("task.status.completed")
        //        sectionLink(reviewElement, 0) must haveHref(
        //          routes.ReviewRegistrationController.displayPage()
        //        )
      }
    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    registrationPage.f(aRegistration(), liabilityStartLink, false)(journeyRequest, messages)
    registrationPage.render(aRegistration(), liabilityStartLink, false, journeyRequest, messages)
  }

}
