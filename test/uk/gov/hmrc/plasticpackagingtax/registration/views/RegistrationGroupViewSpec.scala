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
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.RegType.GROUP
import uk.gov.hmrc.plasticpackagingtax.registration.forms.{Date, LiabilityWeight}
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  MetaData,
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_group
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class RegistrationGroupViewSpec extends UnitViewSpec with Matchers {

  private val LIABILITY_DETAILS                    = 0
  private val NOMINATED_BUSINESS_DETAILS           = 1
  private val NOMINATED_PRIMARY_CONTACT_DETAILS    = 2
  private val OTHER_BUSINESS_DETAILS               = 3
  private val CHECK_AND_SUBMIT                     = 4
  private val registrationPage: registration_group = instanceOf[registration_group]

  private val liabilityStartLink = Call("GET", "/liabilityStartLink")

  private def createView(registration: Registration = aRegistration()): Html =
    registrationPage(registration, liabilityStartLink)

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

        val registration = aRegistration(withRegistrationType(GROUP),
                                         withLiabilityDetails(
                                           LiabilityDetails(weight =
                                                              Some(LiabilityWeight(Some(1000))),
                                                            startDate = None
                                           )
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

        "Nominated organisation details" in {
          val businessElement = view.getElementsByClass("app-task").get(NOMINATED_BUSINESS_DETAILS)

          header(businessElement) must include(
            messages("registrationPage.task.organisation.heading.group")
          )

          sectionName(businessElement, 0) mustBe messages(
            "registrationPage.task.organisation.group"
          )
          sectionStatus(businessElement, 0) mustBe messages("task.status.cannotStartYet")
        }

        "Nominated organisation contact details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(NOMINATED_PRIMARY_CONTACT_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.group")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.group"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.cannotStartYet")
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

        val registration = aRegistration(withRegistrationType(GROUP),
                                         withLiabilityDetails(
                                           LiabilityDetails(
                                             weight = Some(LiabilityWeight(Some(1000))),
                                             startDate = Some(Date(Some(1), Some(4), Some(2022)))
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
            routes.OrganisationDetailsTypeController.displayPage()
          )
        }

        "Nominated organisation contact details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(NOMINATED_PRIMARY_CONTACT_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.group")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.group"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.cannotStartYet")
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
          aRegistration(withRegistrationType(GROUP), withMetaData(MetaData()))

        val view: Html =
          createView(registration)

        "Nominated organisation contact details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(NOMINATED_PRIMARY_CONTACT_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.group")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.group"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.inProgress")
          sectionLink(contactElement, 0) must haveHref(
            routes.ContactDetailsFullNameController.displayPage()
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
          aRegistration(withRegistrationType(GROUP), withMetaData(registrationCompletedMetaData))

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
            routes.OrganisationDetailsTypeController.displayPage()
          )
        }

        "Nominated organisation contact details" in {
          val contactElement =
            view.getElementsByClass("app-task").get(NOMINATED_PRIMARY_CONTACT_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.group")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.group"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.completed")
          sectionLink(contactElement, 0) must haveHref(
            routes.ContactDetailsFullNameController.displayPage()
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
          aRegistration(withRegistrationType(GROUP), withMetaData(inProgressMetaData))
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
          aRegistration(withRegistrationType(GROUP), withMetaData(completedMetaData))
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
    registrationPage.f(aRegistration(), liabilityStartLink)(journeyRequest, messages)
    registrationPage.render(aRegistration(), liabilityStartLink, journeyRequest, messages)
  }

}
