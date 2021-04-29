// ==UserScript==
// @name         PPT AutoComplete
// @namespace    http://tampermonkey.net/
// @version      9.0
// @description
// @author       pmonteiro
// @match        http*://*/plastic-packaging-tax*
// @include      http*://*/identify-your-incorporated-business*
// @grant GM_setValue
// @grant GM_getValue
// @updateURL    https://raw.githubusercontent.com/hmrc/plastic-packaging-tax-frontend/master/tampermonkey/PPT_AutoComplete.js
// ==/UserScript==

/*eslint no-undef: "error"*/

(function() {
    'use strict'
    document.body.appendChild(setup())
})()

function setup() {
    var panel = document.createElement('div')

    panel.appendChild(createQuickButton())
    panel.appendChild(createGRSFeatureFlagsLink())

    return panel
}

function createQuickButton() {

    let button = document.createElement('button')
    button.id='quickSubmit'

    if (!!document.getElementById('global-header')) {
        button.classList.add('button-start', 'govuk-!-display-none-print')
    } else {
        button.classList.add('govuk-button','govuk-!-display-none-print')
    }

    button.style.position = 'absolute'
    button.style.top = '50px'
    button.innerHTML = 'Quick Submit'
    button.onclick = () => completeJourney()

    return button
}

function createGRSFeatureFlagsLink() {

    let a = document.createElement('a')
    a.id='grsFlags'

    a.target = '_blank'
    a.href = '/identify-your-incorporated-business/test-only/feature-switches'
    if (window.location.hostname === 'localhost') {
        a.href = 'http://localhost:9718/identify-your-incorporated-business/test-only/feature-switches'
    } else {
        a.href = '/identify-your-incorporated-business/test-only/feature-switches'
    }
    a.innerText = 'GRS Flags Config'

    a.classList.add('govuk-link','govuk-link--no-visited-state')
    a.style.position = "absolute"
    a.style.top = "100px"

    return a
}

const currentPageIs = (path) => {
    if(path.includes("*")) {
        let matches = window.location.pathname.match(path)
        return matches && window.location.pathname.endsWith(path.slice(-5))
    } else {
        return path === window.location.pathname
    }
}

/*########################     PPT REGISTRATION PAGES     ########################## */
const startPage = () => {
    if (currentPageIs('/plastic-packaging-tax/start-plastic-packaging-tax-registration')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const registrationPage = () => {
    if (currentPageIs('/plastic-packaging-tax/register-plastic-packaging-tax')) {

        let BUSINESS_DETAILS_STATUS = 'li:nth-child(1) .govuk-tag';
        let BUSINESS_DETAILS_LINK = 'li:nth-child(1) .govuk-link';

        let PRIMARY_CONTACT_DETAILS_STATUS = 'li:nth-child(2) .govuk-tag';
        let PRIMARY_CONTACT_DETAILS_LINK = 'li:nth-child(2) .govuk-link';

        let LIABILITY_DETAILS_STATUS = 'li:nth-child(3) .govuk-tag';
        let LIABILITY_DETAILS_LINK = 'li:nth-child(3) .govuk-link';

        if (document.querySelector(BUSINESS_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(BUSINESS_DETAILS_LINK).click()
        } else if (document.querySelector(PRIMARY_CONTACT_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(PRIMARY_CONTACT_DETAILS_LINK).click()
        } else if (document.querySelector(LIABILITY_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(LIABILITY_DETAILS_LINK).click()
        } else {
            document.querySelectorAll('li')[5].getElementsByClassName('govuk-link')[0].click()
        }
    }
}

const honestyDeclaration = () => {
    if (currentPageIs('/plastic-packaging-tax/honesty-declaration')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const organisationBasedInUk = () => {
    if (currentPageIs('/plastic-packaging-tax/organisation-uk-based')) {

		document.getElementById('answer').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const organisationType = () => {
    if (currentPageIs('/plastic-packaging-tax/organisation-type')) {

		document.getElementById('answer').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

/*########################     GRS FUNCTIONS     ########################## */
const grsFeatureFlags = () => {
    if (currentPageIs('/identify-your-incorporated-business/test-only/feature-switches')) {
        document.getElementById('feature-switch.companies-house-stub').checked=true
        document.getElementById('feature-switch.business-verification-stub').checked=true
        document.getElementById('feature-switch.enable-unmatched-ctutr-journey').checked=true
        document.getElementById('feature-switch.enable-IRCT-enrolment-journey').checked=true
        document.getElementById('feature-switch.ct-reference-stub').checked=true
        document.getElementById('feature-switch.des-stub').checked=true

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsCompanyNumber = () => {
    if (currentPageIs('/identify-your-incorporated-business/.*/company-number')) {
        document.getElementById('companyNumber').value = '01234567'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsConfirmCompany = () => {
    if (currentPageIs('/identify-your-incorporated-business/.*/confirm-business-name')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsEnterUtr = () => {
    if (currentPageIs('/identify-your-incorporated-business/.*/ct-utr')) {
        document.getElementById('ctutr').value = '1234567890'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsCheckYourAnswers = () => {
    if (currentPageIs('/identify-your-incorporated-business/.*/check-your-answers-business')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityStartDate = () => {
    if (currentPageIs('/plastic-packaging-tax/liable-date')) {

        document.getElementById('day').value = '01'
        document.getElementById('month').value = '06'
        document.getElementById('year').value = '2022'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityWeight = () => {
    if (currentPageIs('/plastic-packaging-tax/total-packaging-weight')) {

        document.getElementById('totalKg').value = '12000'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityCheckYourAnswers = () => {
    if (currentPageIs('/plastic-packaging-tax/check-plastic-packaging-answers')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactFullName = () => {
    if (currentPageIs('/plastic-packaging-tax/primary-contact-name')) {
        document.getElementById('firstName').value = 'Jack'
        document.getElementById('lastName').value = 'Gatsby'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactJobTitle = () => {
    if (currentPageIs('/plastic-packaging-tax/primary-contact-job-title')) {

        document.getElementById('value').value = 'Chief Financial Officer'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactEmailAddress = () => {
    if (currentPageIs('/plastic-packaging-tax/primary-contact-email')) {

        document.getElementById('value').value = 'ppt@mail.com'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactTelephoneNumber = () => {
     if (currentPageIs('/plastic-packaging-tax/primary-contact-phone-number')) {

         document.getElementById('value').value = '07712345678'
         document.getElementsByClassName('govuk-button')[0].click()
     }
 }

const primaryContactConfirmAddress = () => {
     if (currentPageIs('/plastic-packaging-tax/confirm-primary-contact-address')) {

         document.getElementById('useRegisteredAddress-2').checked = true
         document.getElementsByClassName('govuk-button')[0].click()
     }
 }

const primaryContactAddress = () => {
     if (currentPageIs('/plastic-packaging-tax/primary-contact-business-address')) {

         document.getElementById('addressLine1').value = '2-3 Scala Street'
         document.getElementById('townOrCity').value = 'London'
         document.getElementById('postCode').value = 'W1T 2HN'
         document.getElementsByClassName('govuk-button')[0].click()
     }
 }

const primaryContactCheckYourAnswers = () => {
     if (currentPageIs('/plastic-packaging-tax/check-primary-contact-details')) {

         document.getElementsByClassName('govuk-button')[0].click()
     }
 }

const reviewRegistration = () => {
     if (currentPageIs('/plastic-packaging-tax/review-registration')) {

         document.getElementsByClassName('govuk-button')[0].click()
     }
 }

/*########################     MAIN FUNCTION     ########################## */
const completeJourney = () => {

    // main
    startPage()
    registrationPage()

    // grs pages
    grsFeatureFlags()
    grsCompanyNumber()
    grsConfirmCompany()
    grsEnterUtr()
    grsCheckYourAnswers()

    // Business Details
    organisationBasedInUk()
    organisationType()
    honestyDeclaration()

    // Liability Details
    liabilityStartDate()
    liabilityWeight()
    liabilityCheckYourAnswers()

    // Primary Contact Details
    primaryContactFullName()
    primaryContactJobTitle()
    primaryContactEmailAddress()
    primaryContactTelephoneNumber()
    primaryContactConfirmAddress()
    primaryContactAddress()
    primaryContactCheckYourAnswers()

    //review registration
    reviewRegistration()
}
