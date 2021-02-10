// ==UserScript==
// @name         PPT AutoComplete
// @namespace    http://tampermonkey.net/
// @version      1.0
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
    if (currentPageIs('/plastic-packaging-tax/start')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const registrationPage = () => {
    if (currentPageIs('/plastic-packaging-tax/registration')) {

        if (document.querySelector('li:nth-child(1) .govuk-tag').textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector('li:nth-child(1) .govuk-link').click()
        } else if (document.querySelector('li:nth-child(2) .govuk-tag').textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector('li:nth-child(2) .govuk-link').click()
        }
    }
}

const honestyDeclaration = () => {
    if (currentPageIs('/plastic-packaging-tax/honesty-declaration')) {

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
    if (currentPageIs('/plastic-packaging-tax/liability-start-date')) {

        document.getElementById('day').value = '01'
        document.getElementById('month').value = '06'
        document.getElementById('year').value = '2022'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityWeight = () => {
    if (currentPageIs('/plastic-packaging-tax/liability-weight')) {

        document.getElementById('totalKg').value = '12000'
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
    honestyDeclaration()

    // Liability Details
    liabilityStartDate()
    liabilityWeight()
}
