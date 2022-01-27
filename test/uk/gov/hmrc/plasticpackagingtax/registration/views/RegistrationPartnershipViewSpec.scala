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

package uk.gov.hmrc.plasticpackagingtax.registration.views

import base.unit.UnitViewSpec
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.organisation.{
  routes => organisationRoutes
}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.partner.{routes => partnerRoutes}
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.Date
import uk.gov.hmrc.plasticpackagingtax.registration.forms.liability.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.forms.organisation.PartnerTypeEnum
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  OrganisationDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.task_list_partnership
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class RegistrationPartnershipViewSpec extends UnitViewSpec with Matchers {

  private val LIABILITY_DETAILS                       = 0
  private val BUSINESS_DETAILS                        = 1
  private val PARTNER_DETAILS                         = 2
  private val CHECK_AND_SUBMIT                        = 3
  private val registrationPage: task_list_partnership = instanceOf[task_list_partnership]

  private val liabilityStartLink = Call("GET", "/liabilityStartLink")

  val registrationWithPartnershipDetails = aRegistration(
    withPartnershipDetails(
      Some(
        scottishPartnershipDetails.copy(partners =
          Seq(
            nominatedPartner(PartnerTypeEnum.UK_COMPANY,
                             soleTraderDetails = Some(soleTraderDetails)
            )
          )
        )
      )
    )
  )

  private def createView(registration: Registration = registrationWithPartnershipDetails): Html =
    registrationPage(registration, liabilityStartLink)

  "Registration Single Entity Page view" should {

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
          ).text() mustBe messages("registrationPage.subheading.incomplete")
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections", 0, 4)
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

        "Partnership details" in {
          val businessElement = view.getElementsByClass("app-task").get(BUSINESS_DETAILS)

          header(businessElement) must include(
            messages("registrationPage.task.organisation.heading.partnership")
          )

          sectionName(businessElement, 0) mustBe messages(
            "registrationPage.task.organisation.partnership"
          )
          sectionStatus(businessElement, 0) mustBe messages("task.status.cannotStartYet")
        }

        "Partners details" in {
          val contactElement = view.getElementsByClass("app-task").get(PARTNER_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.partnership")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.partnership"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.cannotStartYet")

          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.partnership.otherPartners"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.cannotStartYet")

        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.cannotStartYet")
        }

      }

      "Eligibility check complete but partnership details not started" when {

        val registration = aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(10000))),
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
          ).text() mustBe messages("registrationPage.completedSections", 1, 4)
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

        "Partnership details" in {
          val organisationElement = view.getElementsByClass("app-task").get(BUSINESS_DETAILS)

          header(organisationElement) must include(
            messages("registrationPage.task.organisation.heading.partnership")
          )

          sectionName(organisationElement, 0) mustBe messages(
            "registrationPage.task.organisation.partnership"
          )
          sectionStatus(organisationElement, 0) mustBe messages("task.status.notStarted")
          sectionLink(organisationElement, 0) must haveHref(
            organisationRoutes.OrganisationDetailsTypeController.displayPage()
          )
        }

        "Partners details" in {
          val contactElement = view.getElementsByClass("app-task").get(PARTNER_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.partnership")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.partnership"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.cannotStartYet")
          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.partnership.otherPartners"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.cannotStartYet")

        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.cannotStartYet")
        }

      }

      "Partnership details complete but nominated partner not started" when {

        val registration = aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(10000))),
                             startDate = Some(Date(Some(1), Some(4), Some(2022)))
            )
          ),
          withPartnershipDetails(Some(generalPartnershipDetails)),
          withNoPrimaryContactDetails()
        )
        val view: Html = createView(registration)

        "application status should reflect the completed sections" in {
          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe messages("registrationPage.subheading.incomplete")
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections", 2, 4)
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

        "Partnership details" in {
          val organisationElement = view.getElementsByClass("app-task").get(BUSINESS_DETAILS)

          header(organisationElement) must include(
            messages("registrationPage.task.organisation.heading.partnership")
          )

          sectionName(organisationElement, 0) mustBe messages(
            "registrationPage.task.organisation.partnership"
          )
          sectionStatus(organisationElement, 0) mustBe messages("task.status.completed")
          sectionLink(organisationElement, 0) must haveHref(
            organisationRoutes.CheckAnswersController.displayPage()
          )
        }

        "Partners details" in {
          val contactElement = view.getElementsByClass("app-task").get(PARTNER_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.partnership")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.partnership"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.notStarted")
          sectionLink(contactElement, 0) must haveHref(
            partnerRoutes.PartnerTypeController.displayNewPartner()
          )
          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.partnership.otherPartners"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.cannotStartYet")

        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.cannotStartYet")
        }

      }

      "Nominated partner complete but other partners not started" when {

        val registration = aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(10000))),
                             startDate = Some(Date(Some(1), Some(4), Some(2022)))
            )
          ),
          withPartnershipDetails(
            Some(
              scottishPartnershipDetails.copy(partners =
                Seq(aLimitedCompanyPartner())
              )
            )
          ),
          withNoPrimaryContactDetails()
        )
        val view: Html = createView(registration)

        "application status should reflect the completed sections" in {
          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe messages("registrationPage.subheading.incomplete")
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections", 2, 4)
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

        "Partnership details" in {
          val organisationElement = view.getElementsByClass("app-task").get(BUSINESS_DETAILS)

          header(organisationElement) must include(
            messages("registrationPage.task.organisation.heading.partnership")
          )

          sectionName(organisationElement, 0) mustBe messages(
            "registrationPage.task.organisation.partnership"
          )
          sectionStatus(organisationElement, 0) mustBe messages("task.status.completed")
          sectionLink(organisationElement, 0) must haveHref(
            organisationRoutes.CheckAnswersController.displayPage()
          )
        }

        "Partners details" in {
          val contactElement = view.getElementsByClass("app-task").get(PARTNER_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.partnership")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.partnership"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.completed")
          sectionLink(contactElement, 0) must haveHref(
            partnerRoutes.PartnerTypeController.displayNewPartner()
          )
          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.partnership.otherPartners"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.notStarted")
          sectionLink(contactElement, 1) must haveHref(
            partnerRoutes.PartnerListController.displayPage()
          )

        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.cannotStartYet")
        }

      }

      "Partners complete but other review and send not started" when {

        val registration = aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(10000))),
                             startDate = Some(Date(Some(1), Some(4), Some(2022)))
            )
          ),
          withPartnershipDetails(
            Some(
              scottishPartnershipDetails.copy(partners =
                Seq(aLimitedCompanyPartner(), aSoleTraderPartner())
              )
            )
          ),
          withNoPrimaryContactDetails()
        )
        val view: Html = createView(registration)

        "application status should reflect the completed sections" in {
          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe messages("registrationPage.subheading.incomplete")
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections", 3, 4)
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

        "Partnership details" in {
          val organisationElement = view.getElementsByClass("app-task").get(BUSINESS_DETAILS)

          header(organisationElement) must include(
            messages("registrationPage.task.organisation.heading.partnership")
          )

          sectionName(organisationElement, 0) mustBe messages(
            "registrationPage.task.organisation.partnership"
          )
          sectionStatus(organisationElement, 0) mustBe messages("task.status.completed")
          sectionLink(organisationElement, 0) must haveHref(
            organisationRoutes.CheckAnswersController.displayPage()
          )
        }

        "Partners details" in {
          val contactElement = view.getElementsByClass("app-task").get(PARTNER_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.partnership")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.partnership"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.completed")
          sectionLink(contactElement, 0) must haveHref(
            partnerRoutes.PartnerTypeController.displayNewPartner()
          )
          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.partnership.otherPartners"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.completed")
          sectionLink(contactElement, 1) must haveHref(
            partnerRoutes.PartnerListController.displayPage()
          )

        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.notStarted")
          sectionLink(reviewElement, 0) must haveHref(
            routes.ReviewRegistrationController.displayPage()
          )
        }

      }

      "All sections complete" when {

        val registrationCompletedMetaData =
          aRegistration().metaData.copy(registrationReviewed = true, registrationCompleted = true)
        val completeRegistration =
          aRegistration(
            withPartnershipDetails(
              Some(
                scottishPartnershipDetails.copy(partners =
                  Seq(aLimitedCompanyPartner(), aSoleTraderPartner())
                )
              )
            ),
            withMetaData(registrationCompletedMetaData)
          )

        val view: Html =
          createView(completeRegistration)

        "application status should reflect the completed sections" in {

          view.getElementsByClass("govuk-heading-s govuk-!-margin-bottom-2").get(
            0
          ).text() mustBe messages("registrationPage.subheading.complete")
          view.getElementsByClass("govuk-body govuk-!-margin-bottom-7").get(
            0
          ).text() mustBe messages("registrationPage.completedSections",
                                   completeRegistration.numberOfCompletedSections,
                                   4
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

        "Partnership details" in {
          val organisationElement = view.getElementsByClass("app-task").get(BUSINESS_DETAILS)

          header(organisationElement) must include(
            messages("registrationPage.task.organisation.heading.partnership")
          )

          sectionName(organisationElement, 0) mustBe messages(
            "registrationPage.task.organisation.partnership"
          )
          sectionStatus(organisationElement, 0) mustBe messages("task.status.completed")
          sectionLink(organisationElement, 0) must haveHref(
            organisationRoutes.CheckAnswersController.displayPage()
          )
        }

        "Partners details" in {
          val contactElement = view.getElementsByClass("app-task").get(PARTNER_DETAILS)

          header(contactElement) must include(
            messages("registrationPage.task.contactDetails.heading.partnership")
          )

          sectionName(contactElement, 0) mustBe messages(
            "registrationPage.task.contactDetails.partnership"
          )
          sectionStatus(contactElement, 0) mustBe messages("task.status.completed")
          sectionLink(contactElement, 0) must haveHref(
            partnerRoutes.PartnerTypeController.displayNewPartner()
          )
          sectionName(contactElement, 1) mustBe messages(
            "registrationPage.task.contactDetails.partnership.otherPartners"
          )
          sectionStatus(contactElement, 1) mustBe messages("task.status.completed")
          sectionLink(contactElement, 1) must haveHref(
            partnerRoutes.PartnerListController.displayPage()
          )
        }

        "Review and send" in {
          val reviewElement = view.getElementsByClass("app-task").get(CHECK_AND_SUBMIT)

          header(reviewElement) must include(messages("registrationPage.task.review.heading"))

          sectionName(reviewElement, 0) mustBe messages("registrationPage.task.review")
          sectionStatus(reviewElement, 0) mustBe messages("task.status.completed")
          sectionLink(reviewElement, 0) must haveHref(
            routes.ReviewRegistrationController.displayPage()
          )
        }

      }

    }
  }

  override def exerciseGeneratedRenderingMethods() = {
    registrationPage.f(registrationWithPartnershipDetails, liabilityStartLink)(journeyRequest,
                                                                               messages
    )
    registrationPage.render(registrationWithPartnershipDetails,
                            liabilityStartLink,
                            journeyRequest,
                            messages
    )
  }

}
