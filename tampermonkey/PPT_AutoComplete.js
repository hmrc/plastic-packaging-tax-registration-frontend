// ==UserScript==
// @name         PPT Registration AutoComplete
// @namespace    http://tampermonkey.net/
// @version      15.20
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

function optionSelected(option, value) {
    return GM_getValue(option) == value;
}

function setup() {
    var panel = document.createElement('div')
    panel.style.position = 'absolute'
    panel.style.top = '50px'
    panel.style.lineHeight = '200%'

    panel.appendChild(createQuickButton())
    panel.appendChild(createAutoCompleteCheckbox())
    panel.appendChild(createDropDown("Journey", ["Single","Group"]))
    panel.appendChild(createDropDown("Organisation", ["UKCompany","Partnership","OverseasUK"]))
    panel.appendChild(createGRSFeatureFlagsLink())
    panel.appendChild(document.createElement('br'))
    panel.appendChild(createSoleTraderGRSFeatureFlagsLink())
    panel.appendChild(document.createElement('br'))
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

    button.innerHTML = 'Quick Submit'
    button.onclick = () => completeJourney(true)

    return button
}

function createDropDown(name, options) {
    var panel = document.createElement("div");

    var id = "my" + name;

    var label = document.createElement("label");
    label.innerText = name + ":";
    label.setAttribute("for", id);
    panel.appendChild(label);

    // create and append select list
    var selectList = document.createElement("select");
    selectList.id = id;
    selectList.className = "govuk-!-display-none-print";
    panel.appendChild(selectList);

    // create and append the options
    options.forEach(item => {
            var option = document.createElement("option");
            option.value = item;
            option.text = item;

            if(GM_getValue(name) == item) option.selected = true;

            selectList.appendChild(option);
        }
    )

    selectList.onchange = function (e) {
        GM_setValue(name, this.value);
    };

    return panel;
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
    return window.location.pathname.match(RegExp(path))
}

/*########################     PPT REGISTRATION PAGES     ########################## */
const startPage = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/start')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const registrationPage = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/task-list')) {

        let LIABILITY_DETAILS_STATUS = 'li.app-task:nth-child(1) .govuk-tag';
        let LIABILITY_DETAILS_LINK = 'li.app-task:nth-child(1) .govuk-link';

        let BUSINESS_DETAILS_STATUS = 'li.app-task:nth-child(2) li:nth-child(1) .govuk-tag';
        let BUSINESS_DETAILS_LINK = 'li.app-task:nth-child(2) li:nth-child(1) .govuk-link';

        let PRIMARY_CONTACT_DETAILS_STATUS = 'li.app-task:nth-child(2) li:nth-child(2) .govuk-tag';
        let PRIMARY_CONTACT_DETAILS_LINK = 'li.app-task:nth-child(2) li:nth-child(2) .govuk-link';

        let REVIEW_STATUS = 'li.app-task:nth-child(3) .govuk-tag';
        let REVIEW_LINK = 'li.app-task:nth-child(3) .govuk-link';

        let REVIEW_GROUP_STATUS = 'li.app-task:nth-child(4) .govuk-tag';
        let REVIEW_GROUP_LINK = 'li.app-task:nth-child(4) .govuk-link';

        if (document.querySelector(LIABILITY_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(LIABILITY_DETAILS_LINK).click()
        } else if (document.querySelector(BUSINESS_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(BUSINESS_DETAILS_LINK).click()
        } else if (document.querySelector(PRIMARY_CONTACT_DETAILS_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(PRIMARY_CONTACT_DETAILS_LINK).click()
        } else if (document.querySelector(REVIEW_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(REVIEW_LINK).click()
        } else if (document.querySelector(REVIEW_GROUP_STATUS).textContent.trim().toUpperCase() !== 'COMPLETED') {
            document.querySelector(REVIEW_GROUP_LINK).click()
        }
    }
}

const organisationType = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-representative-member-type') ||
      currentPageIs('/register-for-plastic-packaging-tax/organisation-type')) {

        if(optionSelected("Organisation", "OverseasUK")){
            if(optionSelected("Journey", "Single")){
                document.getElementById('answer-2').checked = true
            } else {
                document.getElementById('answer-3').checked = true
            }
        } else if(optionSelected("Organisation", "Partnership")){
            if(optionSelected("Journey", "Single")){
                document.getElementById('answer-4').checked = true
            } else{
                document.getElementById('answer-2').checked = true
            }
        } else {
            document.getElementById('answer').checked = true
        }

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const partnership = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/partnership-type$')) {
        document.getElementById('answer').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const partnershipName = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/partnership-name')) {
        document.getElementById('value').value = 'Partners in Plastic'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const confirmBusinessAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/confirm-address')) {
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
        document.getElementById('companyNumber').value = generateCrn()

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
        document.getElementById('date-of-birth-day').value = '01'
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

const grsPartnershipCrn = () => {
    if (currentPageIs('/identify-your-partnership/.*/company-registration-number')) {
        document.getElementById('companyNumber').value = generateCrn()

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const grsPartnershipConfirmCompany = () => {
    if (currentPageIs('/identify-your-partnership/.*/confirm-company-name')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

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

const partnerOrganisationList = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/partnership-partners-list')) {

        if (document.getElementById('main-content').getElementsByTagName('li').length >= 2) {
            document.getElementById('addPartner-2').checked = true
        } else {
            document.getElementById('addPartner').checked = true
        }
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const partnerOrganisation = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/partnership-nominated-partner-type') ||
        currentPageIs('/register-for-plastic-packaging-tax/partnership-partner-type') ) {

        document.getElementById('answer').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const partnerContactName = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/.*partner-contact-name')) {

        document.getElementById('firstName').value = "James"
        document.getElementById('lastName').value = "Sparrow"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const partnerJobTitle = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/partner-job-title')) {
        document.getElementById('value').value = "Director"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const partnerContactEmailAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/partner-email-address')
        || currentPageIs('/register-for-plastic-packaging-tax/amend-partner-email')
        || currentPageIs('/register-for-plastic-packaging-tax/amend-add-partner-contact-email')) {

        document.getElementById('value').value = "test@test.com"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const partnerContactPhoneNumber = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/partner-phone-number')
    || currentPageIs('/register-for-plastic-packaging-tax/amend-partner-phone-number')
    || currentPageIs('/register-for-plastic-packaging-tax/amend-add-partner-contact-telephone')) {

        document.getElementById('value').value = "07712345677"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const checkAnswers = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/.*check-answers')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}
/* ####################### PPT */

/* ####################### Pre-launch Liability */

const liabilityStartDate = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/liable-date')) {

        document.getElementById('day').value = '01'
        document.getElementById('month').value = '04'
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
    if (currentPageIs('/register-for-plastic-packaging-tax/weight-next-12-months')) {

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

/* ####################### Post-launch Liability */

const liabilityHasYourGroupExceeded = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/threshold-from-1-april-2022')) {

        document.getElementById('value-yes').checked = true
        document.getElementById('exceeded-threshold-weight-date.day').value = '01'
        document.getElementById('exceeded-threshold-weight-date.month').value = '04'
        document.getElementById('exceeded-threshold-weight-date.year').value = '2022'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

/* ####################### Post-April 23 Liability */

const liabilityForwardLook = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/threshold-next-30-days')) {

        // Post-April 23
        if (document.getElementById('value')) {
            document.getElementById('value').checked = true
        }
        // Pre-April 23
        else {
            document.getElementById('value-yes').checked = true
            document.getElementById('expect-to-exceed-threshold-weight-date.day').value = '01'
            document.getElementById('expect-to-exceed-threshold-weight-date.month').value = '04'
            document.getElementById('expect-to-exceed-threshold-weight-date.year').value = '2022'
        }

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityForwardLookDate = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/date-expected-to-meet-threshold')) {

        document.getElementById('expect-to-exceed-threshold-weight-date.day').value = '01'
        document.getElementById('expect-to-exceed-threshold-weight-date.month').value = '04'
        document.getElementById('expect-to-exceed-threshold-weight-date.year').value = '2022'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityBackwardLook = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/threshold-last-12-months')) {

        document.getElementById('value').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityBackwardLookDate = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/date-met-threshold')) {

        document.getElementById('exceeded-threshold-weight-date.day').value = '01'
        document.getElementById('exceeded-threshold-weight-date.month').value = '04'
        document.getElementById('exceeded-threshold-weight-date.year').value = '2022'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const liabilityTaxStartDate = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/tax-start-date')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const registrationType = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/registration-type')||currentPageIs('/register-for-plastic-packaging-tax/single-or-group')) {

        if(optionSelected("Journey", "Single")||optionSelected("Journey", "Partnership")){
            document.getElementById('value').checked = true
        } else {
            document.getElementById('value-2').checked = true
        }
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const membersUnderGroupControl = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-same-control')) {

        document.getElementById('value').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const groupCannotApply = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-cannot-apply')) {

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
    if (currentPageIs('/register-for-plastic-packaging-tax/group-member-type')) {

        document.getElementById('answer').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const groupMemberContactName = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-member-contact-name/.*')) {

        document.getElementById('firstName').value = "James"
        document.getElementById('lastName').value = "Sparrow"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const amendGroupMemberContactName = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/amend-group-member-contact-name/.*')) {

        document.getElementById('firstName').value = "John"
        document.getElementById('lastName').value = "Pigeon"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const groupMemberContactEmailAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-member-contact-email/.*')) {

        document.getElementById('value').value = "test@test.com"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const amendGroupMemberContactEmailAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/amend-group-member-contact-email/.*')) {

        document.getElementById('value').value = "testamend@test.com"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const groupMemberContactPhoneNumber = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/group-member-contact-telephone/.*')) {

        document.getElementById('value').value = "07712345677"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const amendGroupMemberContactPhoneNumber = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/amend-group-member-contact-phone-number/.*')) {

        document.getElementById('value').value = "07834123456"
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactFullName = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/main-contact-name')) {
        document.getElementById('value').value = 'Jack Gatsby'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const amendContactFullName = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/amend-contact-name')) {
        document.getElementById('value').value = 'John Gatensby'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactJobTitle = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/main-contact-job-title')) {

        document.getElementById('value').value = 'Chief Financial Officer'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const amendContactJobTitle = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/amend-job-title')) {

        document.getElementById('value').value = 'Cost Estimator'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactEmailAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/main-contact-email') || currentPageIs('/register-for-plastic-packaging-tax/amend-email') ) {

        document.getElementById('value').value = 'ppt@mail.com'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const emailAddressPasscode = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/.*confirm-email-code')) {
        var passcode = getPasscode()

        document.getElementById('value').value = passcode
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const emailAddressPasscodeConfirmation = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/.*email-confirmed')) {
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactTelephoneNumber = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/main-contact-telephone')) {

        document.getElementById('value').value = '07712345678'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const amendContactTelephoneNumber = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/amend-phone-number')) {

        document.getElementById('value').value = '07867123456'
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const primaryContactConfirmAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/confirm-contact-address')) {

        document.getElementById('useRegisteredAddress-2').checked = true
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const reviewRegistration = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/review-registration') ||
        currentPageIs('/register-for-plastic-packaging-tax/amend-registration')) {

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

/*########################     ADDRESS CAPTURE COMPONENT     ########################## */

const isUkAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/uk-address')) {
        document.getElementById('ukAddress').click()
        document.getElementById('submit').click()
    }
}

const pptAddressCapture = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/address')) {
        document.getElementById('addressLine1').value = '2-3 Scala Street'
        document.getElementById('addressLine2').value = 'Soho'
        document.getElementById('townOrCity').value = 'London'
        document.getElementById('postCode').value = 'W1T 2HN'
        document.getElementById("countryCode-select").getElementsByTagName("option")[185].selected = "selected"
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

const enrolmentPptReference = () => {
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

const enrolmentIsUkAddress = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-uk-address')) {
        document.getElementById('value').checked = true

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const enrolmentUkPostcode = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-postcode')) {
        document.getElementById('value').value = 'AB1 2CD'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const enrolmentRegistrationDate = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/enrolment-registration-date')) {
        document.getElementById('date.day').value = '10'
        document.getElementById('date.month').value = '10'
        document.getElementById('date.year').value = '2021'

        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const enrolmentVerificationFailure = () => {
    if (currentPageIs('/register-for-plastic-packaging-tax/registration-number-already-used')) {
        document.getElementsByClassName('govuk-button')[0].click()
    }
}

const generateCrn = () => {
    return Math.floor(10000000 + Math.random() * 90000000)
}
/*########################     MAIN FUNCTION     ########################## */registration-number-already-used
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
    grsPartnershipCrn()
    grsPartnershipConfirmCompany()
    grsPartnershipUtr()
    grsPartnershipPostcode()
    grsPartnershipCheckYourAnswers()
    partnerOrganisationList()
    partnerOrganisation()
    partnerContactName()
    partnerJobTitle()
    partnerContactEmailAddress()
    emailAddressPasscode()
    emailAddressPasscodeConfirmation()
    partnerContactPhoneNumber()
    checkAnswers()

    // Business Details
    organisationType()
    partnership()
    partnershipName()
    confirmBusinessAddress()

    // Liability Details
    liabilityLiableDate()
    liabilityStartDate()
    liabilityHasYourGroupExceeded()
    liabilityForwardLook()
    liabilityForwardLookDate()
    liabilityBackwardLook()
    liabilityBackwardLookDate()
    liabilityTaxStartDate()
    liabilityWeight()
    liabilityExpectedWeight()
    liabilityCheckYourAnswers()
    registrationType()
    checkAnswers()
    membersUnderGroupControl()
    groupCannotApply()

    // Primary Contact Details
    primaryContactFullName()
    primaryContactJobTitle()
    primaryContactEmailAddress()
    emailAddressPasscode()
    emailAddressPasscodeConfirmation()
    primaryContactTelephoneNumber()
    primaryContactConfirmAddress()
    checkAnswers()

    // groups
    groupOrganisationList()
    groupMemberOrganisation()
    groupMemberContactName()
    groupMemberContactEmailAddress()
    groupMemberContactPhoneNumber()
    checkAnswers()

    //review registration
    if(manualJourney){
        reviewRegistration()
    }

    // address capture component
    isUkAddress()
    pptAddressCapture()

    // address lookup
    addressLookupLookup()
    addressLookupChoose()
    addressLookupConfirm()
    addressLookupEdit()

    //enrolment
    enrolmentPptReference()
    verifyOrganisation()
    enrolmentIsUkAddress()
    enrolmentUkPostcode()
    enrolmentRegistrationDate()
    checkAnswers()
    enrolmentVerificationFailure()

    // amend pages
    amendContactFullName()
    amendContactJobTitle()
    amendContactTelephoneNumber()
    amendGroupMemberContactName()
    amendGroupMemberContactEmailAddress()
    amendGroupMemberContactPhoneNumber()
}
