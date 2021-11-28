// ==UserScript==
// @name         PPT Registration AutoComplete
// @namespace    http://tampermonkey.net/
// @version      15.1
// @description
// @author       pmonteiro
// @match        http*://*/register-for-plastic-packaging-tax*
// @include      http*://*/identify-your-incorporated-business*
// @include      http*://*/identify-your-sole-trader-business*
// @include      http*://*/identify-your-partnership*
// @include      http*://*/lookup-address/*
// @grant GM_setValue
// @grant GM_getValue
// @updateURL    https://raw.githubusercontent.com/hmrc/plastic-packaging-tax-frontend/master/tampermonkey/PPT_AutoComplete.js
// ==/UserScript==

/*eslint no-undef: "error"*/

(function() {
    'use strict'
    document.body.appendChild(setup())

    setTimeout(function() {
        if(getAutocomplete()){
            completeJourney(false)
        }
    }, 100)
})()

function setAutocomplete(choice) {
    GM_setValue("autoComplete", choice);
}

function getAutocomplete() {
    return GM_getValue("autoComplete");
}

function setup() {
    var panel = document.createElement('div')

    panel.appendChild(createQuickButton())
    panel.appendChild(createAutoCompleteCheckbox())
    panel.appendChild(createGRSFeatureFlagsLink())
    panel.appendChild(createSoleTraderGRSFeatureFlagsLink())
    panel.appendChild(createPartnershipGRSFeatureFlagsLink())

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
    button.onclick = () => completeJourney(true)

    return button
}

function createAutoCompleteCheckbox() {

    let chkBox = document.createElement('input')
    chkBox.id='autoComplete'
    chkBox.type = "checkbox"
    chkBox.checked = getAutocomplete()

    chkBox.onchange = function (e) { setAutocomplete(this.checked); };

    let panel = document.createElement("div");
    panel.appendChild(chkBox);
    let label = document.createElement("label");
    label.innerText = "Auto complete";
    label.setAttribute("for", "autoComplete");
    panel.appendChild(label);

    panel.style.position = 'absolute'
    panel.style.top = '100px'

    return panel
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
    a.style.top = "150px"

    return a
}

function createSoleTraderGRSFeatureFlagsLink() {

    let a = document.createElement('a')
    a.id='grsFlags'

    a.target = '_blank'
    a.href = '/identify-your-sole-trader-business/test-only/feature-switches'
    if (window.location.hostname === 'localhost') {
        a.href = 'http://localhost:9717/identify-your-sole-trader-business/test-only/feature-switches'
    } else {
        a.href = '/identify-your-sole-trader-business/test-only/feature-switches'
    }
    a.innerText = 'Sole Trader GRS Flags Config'

    a.classList.add('govuk-link','govuk-link--no-visited-state')
    a.style.position = "absolute"
    a.style.top = "200px"

    return a
}

function createPartnershipGRSFeatureFlagsLink() {

    let a = document.createElement('a')
    a.id='grsFlags'

    a.target = '_blank'
    a.href = '/identify-your-partnership/test-only/feature-switches'
    if (window.location.hostname === 'localhost') {
        a.href = 'http://localhost:9722/identify-your-partnership/test-only/feature-switches'
    } else {
        a.href = '/identify-your-partnership/test-only/feature-switches'
    }
    a.innerText = 'Partnership GRS Flags Config'

    a.classList.add('govuk-link','govuk-link--no-visited-state')
    a.style.position = "absolute"
    a.style.top = "250px"

    return a
}

function getPasscode(){
    var url;
    if (window.location.hostname === 'localhost') {
        url = 'http://localhost:8503/register-for-plastic-packaging-tax/test-only/passcode'
    } else {
        url = '/register-for-plastic-packaging-tax/test-only/passcode'
    }
    var xmlHttp = new XMLHttpRequest();
    xmlHttp. open("GET", url, false); // false for synchronous request.
    xmlHttp. send();
    var passcodes = JSON.parse(xmlHttp.responseText).passcodes
    return passcodes[passcodes.length-1].passcode
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
    if (currentPageIs('/register-for-plastic-packaging-tax/start')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const registrationPage = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/task-list')) {

        let LIABILITY_DETAILS_STATUS = 'li:nth-child(1) .govuk-tag';
        let LIABILITY_DETAILS_LINK = 'li:nth-child(1) .govuk-link';

        let BUSINESS_DETAILS_STATUS = 'li:nth-child(2) .govuk-tag';
        let BUSINESS_DETAILS_LINK = 'li:nth-child(2) .govuk-link';

        let PRIMARY_CONTACT_DETAILS_STATUS = 'li:nth-child(3) .govuk-tag';
        let PRIMARY_CONTACT_DETAILS_LINK = 'li:nth-child(3) .govuk-link';

        let REVIEW_STATUS = 'li:nth-child(4) .govuk-tag';
        let REVIEW_LINK = 'li:nth-child(4) .govuk-link';

        if (document.querySelector(LIABILITY_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(LIABILITY_DETAILS_LINK).click()
        } else if (document.querySelector(BUSINESS_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(BUSINESS_DETAILS_LINK).click()
        } else if (document.querySelector(PRIMARY_CONTACT_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(PRIMARY_CONTACT_DETAILS_LINK).click()
        } else if (document.querySelector(REVIEW_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(REVIEW_LINK).click()
        }
    }
}

const organisationType = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/organisation-type')) {

        document.getElementById('answer').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

/*########################     GRS FUNCTIONS     ########################## */
const grsUkLimitedFeatureFlags = () => {
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

const grsSoleTraderFeatureFlags = () => {
    if (currentPageIs('/identify-your-sole-trader-business/test-only/feature-switches')) {
        document.getElementById('feature-switch.authenticator-stub').checked=true
        document.getElementById('feature-switch.business-verification-stub').checked=true
        document.getElementById('feature-switch.sa-reference-stub').checked=true
        document.getElementById('feature-switch.des-stub').checked=true

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsPartnershipFeatureFlags = () => {
    if (currentPageIs('/identify-your-partnership/test-only/feature-switches')) {
        document.getElementById('feature-switch.business-verification-stub').checked=true
        document.getElementById('feature-switch.partnership-known-facts-stub').checked=true
        document.getElementById('feature-switch.register-with-identifiers-stub').checked=true

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

/* ####################### GRS UK COMPANY */
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

/* ####################### GRS SOLE TRADER */

const grsStFirstLastName = () => {
    if (currentPageIs('/identify-your-sole-trader-business/.*/full-name')) {
        document.getElementById('first-name').value = 'James'
        document.getElementById('last-name').value = 'Bond'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsStDob = () => {
    if (currentPageIs('/identify-your-sole-trader-business/.*/date-of-birth')) {
        document.getElementById('date-of-birth').value = '01'
        document.getElementById('date-of-birth-month').value = '06'
        document.getElementById('date-of-birth-year').value = '1977'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsStNino = () => {
    if (currentPageIs('/identify-your-sole-trader-business/.*/national-insurance-number')) {
        document.getElementById('nino').value = 'SE123456C'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsStSaUtr = () => {
    if (currentPageIs('/identify-your-sole-trader-business/.*/sa-utr')) {
        document.getElementById('sa-utr').value = '1234567890'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsStCheckYourAnswers = () => {
    if (currentPageIs('/identify-your-sole-trader-business/.*/check-your-answers-business')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

/* ####################### GRS PARTNERSHIP */

const grsPartnershipUtr = () => {
    if (currentPageIs('/identify-your-partnership/.*/sa-utr')) {
        console.log('SA-UTR')
        document.getElementById('sa-utr').value = '1234567890'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsPartnershipPostcode = () => {
    if (currentPageIs('/identify-your-partnership/.*/self-assessment-postcode')) {
        document.getElementById('postcode').value = 'AA11AA'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsPartnershipCheckYourAnswers = () => {
    if (currentPageIs('/identify-your-partnership/.*/check-your-answers-business')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

/* ####################### PPT */

const liabilityStartDate = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/liable-date')) {

        document.getElementById('day').value = '01'
        document.getElementById('month').value = '06'
        document.getElementById('year').value = '2022'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityLiableDate = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/liability-date')) {

        document.getElementById('answer').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityWeight = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/total-packaging-weight')) {

        document.getElementById('totalKg').value = '12000'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityExpectedWeight = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/packaging-weight-next-12months')) {

        document.getElementById('answer').checked = true
        document.getElementById('totalKg').value = '12000'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const registrationType = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/registration-type')) {

        document.getElementById('value').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const membersUnderGroupControl = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-same-control')) {

        document.getElementById('value').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityCheckYourAnswers = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/check-plastic-packaging-answers')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const groupOrganisationList = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-organisation-list')) {

        document.getElementById('addOrganisation-2').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const groupMemberOrganisation = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-member-organisation')) {

        document.getElementById('answer').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactFullName = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/main-contact-name')) {
        document.getElementById('value').value = 'Jack Gatsby'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactJobTitle = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/main-contact-job-title')) {

        document.getElementById('value').value = 'Chief Financial Officer'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactEmailAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/main-contact-email')) {

        document.getElementById('value').value = 'ppt@mail.com'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactEmailAddressPasscode = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/confirm-email-code')) {
        var passcode = getPasscode()

        document.getElementById('value').value = passcode
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactEmailAddressPasscodeConfirmation = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/email-confirmed')) {
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactTelephoneNumber = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/main-contact-telephone')) {

        document.getElementById('value').value = '07712345678'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactConfirmAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/confirm-contact-address')) {

        document.getElementById('useRegisteredAddress-2').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enter-contact-address')) {

        document.getElementById('addressLine1').value = '2-3 Scala Street'
        document.getElementById('addressLine2').value = 'Soho'
        document.getElementById('townOrCity').value = 'London'
        document.getElementById('postCode').value = 'W1T 2HN'
        document.getElementById("countryCode-select").getElementsByTagName("option")[185].selected = "selected"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactCheckYourAnswers = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/contact-check-answers')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const reviewRegistration = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/review-registration')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

/*########################     ADDRESS LOOKUP PAGES     ########################## */
const addressLookupLookup = () => {
    if (currentPageIs('.*/lookup-address/.*/lookup')) {

        if (window.location.host.includes("localhost")) {
            document.getElementById("postcode").value = "ZZ1 1ZZ";
        }
        else{
            document.getElementById("postcode").value = "WS1 2AB";
        }
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const addressLookupChoose = () => {
    if (currentPageIs('.*/lookup-address/.*/select')) {

        document.getElementById('addressId').checked = true

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const addressLookupConfirm = () => {
    if (currentPageIs('.*/lookup-address/.*/confirm')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const addressLookupEdit = () => {
    if (currentPageIs('.*/lookup-address/.*/edit')) {

        document.getElementById("line1").value = "Unit 42";
        document.getElementById("line2").value = "West Industrial Estate";
        document.getElementById("town").value = "Walsall";
        document.getElementById("postcode").value = "WS1 2AB";

        document.getElementsByClassName('govuk-button')[0].click()
    }
}


/*########################     PPT ENROLMENT PAGES     ########################## */

const pptReference = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-ppt-reference')) {
        document.getElementById('value').value = 'XMPPT0001234567'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const verifyOrganisation = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-security')) {
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const isUkAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-uk-address')) {
        document.getElementById('value').checked = true

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const ukPostcode = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-postcode')) {
        document.getElementById('value').value = 'AB1 2CD'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const registrationDate = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-registration-date')) {
        document.getElementById('date.day').value = '10'
        document.getElementById('date.month').value = '10'
        document.getElementById('date.year').value = '2021'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const enrolmentCheckAnswers = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-check-answers')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const verificationFailure = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-not-verified')) {
        document.getElementsByClassName('govuk-button')[0].click()
    }
}
/*########################     MAIN FUNCTION     ########################## */
function completeJourney(manualJourney) {

    // main
    startPage()

    if(manualJourney){
        registrationPage()
    }

    // grs uk company pages
    grsUkLimitedFeatureFlags()
    grsSoleTraderFeatureFlags()
    grsPartnershipFeatureFlags()
    grsCompanyNumber()
    grsConfirmCompany()
    grsEnterUtr()
    grsCheckYourAnswers()

    // grs sole trader pages
    grsStFirstLastName()
    grsStDob()
    grsStNino()
    grsStSaUtr()
    grsStCheckYourAnswers()

    // grs partnership pages
    grsPartnershipUtr()
    grsPartnershipPostcode()
    grsPartnershipCheckYourAnswers()

    // Business Details
    organisationType()

    // Liability Details
    liabilityLiableDate()
    liabilityStartDate()
    liabilityWeight()
    liabilityExpectedWeight()
    liabilityCheckYourAnswers()
    registrationType()
    membersUnderGroupControl()

    // Primary Contact Details
    primaryContactFullName()
    primaryContactJobTitle()
    primaryContactEmailAddress()
    primaryContactEmailAddressPasscode()
    primaryContactEmailAddressPasscodeConfirmation()
    primaryContactTelephoneNumber()
    primaryContactConfirmAddress()
    primaryContactAddress()
    primaryContactCheckYourAnswers()

    // groups
    groupOrganisationList()
    groupMemberOrganisation()

    //review registration
    if(manualJourney){
        reviewRegistration()
    }

    // address lookup
    addressLookupLookup()
    addressLookupChoose()
    addressLookupConfirm()
    addressLookupEdit()

    //enrolment
    pptReference()
    verifyOrganisation()
    isUkAddress()
    ukPostcode()
    registrationDate()
    enrolmentCheckAnswers()
    verificationFailure()
}
