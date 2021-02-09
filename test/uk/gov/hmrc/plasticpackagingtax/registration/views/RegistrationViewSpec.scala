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
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import play.twirl.api.Html
import uk.gov.hmrc.plasticpackagingtax.registration.controllers.routes
import uk.gov.hmrc.plasticpackagingtax.registration.forms.LiabilityWeight
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.{
  LiabilityDetails,
  Registration
}
import uk.gov.hmrc.plasticpackagingtax.registration.views.html.registration_page
import uk.gov.hmrc.plasticpackagingtax.registration.views.tags.ViewTest

@ViewTest
class RegistrationViewSpec extends UnitViewSpec with Matchers {

  private val registrationPage: registration_page = instanceOf[registration_page]

  private def createView(registration: Registration = aRegistration()): Html =
    registrationPage(registration)

  "Registration Page view" should {

    "have proper messages for labels" in {

      messages must haveTranslationFor("registrationPage.title")
      messages must haveTranslationFor("registrationPage.organisationDetails")
      messages must haveTranslationFor("registrationPage.businessInfo")
      messages must haveTranslationFor("registrationPage.plasticPackagingDetails")
      messages must haveTranslationFor("registrationPage.plasticPackagingInfo")
      messages must haveTranslationFor("registrationPage.businessContactDetails")
      messages must haveTranslationFor("registrationPage.businessContactInfo")
      messages must haveTranslationFor("registrationPage.applicantContactDetails")
      messages must haveTranslationFor("registrationPage.applicantContactInfo")
      messages must haveTranslationFor("registrationPage.declaration.header")
      messages must haveTranslationFor("registrationPage.declaration.description")
      messages must haveTranslationFor("registrationPage.declaration.buttonName")
      messages must haveTranslationFor("task.status.notStarted")
      messages must haveTranslationFor("task.status.inProgress")
      messages must haveTranslationFor("task.status.completed")
    }

    val view: Html = createView()

    "display title" in {

      view.select("title").text() must include(messages("registrationPage.title"))
    }

    "display header" in {

      view.getElementById("title") must containMessage("registrationPage.title")
    }

    "display 'list of tasks' section" in {
      val view: Html = createView(
        aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(1000))), startDate = None)
          ),
          withIncorpJourneyId(Some("123"))
        )
      )

      validateTask(view,
                   0,
                   "registrationPage.organisationDetails",
                   "registrationPage.businessInfo",
                   "task.status.completed",
                   routes.HonestyDeclarationController.displayPage()
      )
      validateTask(view,
                   1,
                   "registrationPage.plasticPackagingDetails",
                   "registrationPage.plasticPackagingInfo",
                   "task.status.inProgress",
                   routes.LiabilityStartDateController.displayPage()
      )
      validateTask(view,
                   2,
                   "registrationPage.businessContactDetails",
                   "registrationPage.businessContactInfo",
                   "task.status.notStarted",
                   routes.RegistrationController.displayPage()
      )
      validateTask(view,
                   3,
                   "registrationPage.applicantContactDetails",
                   "registrationPage.applicantContactInfo",
                   "task.status.cannotStartYet",
                   routes.RegistrationController.displayPage()
      )
    }

    "display 'organisation details' section as NotStarted when incorpId not exist" in {
      val view: Html = createView(
        aRegistration(
          withLiabilityDetails(
            LiabilityDetails(weight = Some(LiabilityWeight(Some(1000))), startDate = None)
          ),
          withIncorpJourneyId(None)
        )
      )

      validateTask(view,
                   0,
                   "registrationPage.organisationDetails",
                   "registrationPage.businessInfo",
                   "task.status.notStarted",
                   routes.HonestyDeclarationController.displayPage()
      )
    }

    def validateTask(
      view: Html,
      index: Int,
      title: String,
      description: String,
      tagStatus: String,
      href: Call
    ) = {
      val taskListElement       = view.getElementsByClass("app-task-list").get(0)
      val taskListElementHeader = taskListElement.getElementsByClass("app-task-list__section")
      val taskItems             = taskListElement.getElementsByClass("app-task-list__item")

      taskListElementHeader.get(index).text() must include(messages(title))
      val taskItem = taskItems.get(index)
      taskItem.text must include(messages(description))
      taskItem.getElementsByClass("govuk-tag").text() must include(messages(tagStatus))
      taskItem.getElementsByClass("govuk-link").get(0) must haveHref(href)
    }

    "review declaration section" in {
      view.getElementById("review-declaration").text() must include(
        messages("registrationPage.declaration.header")
      )
      view.getElementById("review-declaration-element").text() must include(
        messages("registrationPage.declaration.description")
      )
    }

    "display 'Review Declaration' button" in {

      view.getElementsByClass("govuk-button").first() must containMessage(
        "registrationPage.declaration.buttonName"
      )
      view.getElementsByClass("govuk-button").first() must haveHref("")
    }
  }
}
