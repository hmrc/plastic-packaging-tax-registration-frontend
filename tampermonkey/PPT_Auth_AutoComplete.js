// ==UserScript==
// @name     Plastic Packaging Tax Registration Authorisation
// @namespace  http://tampermonkey.net/
// @version   5.0
// @description Auth Wizard autocomplete script for PPT
// @author    pmonteiro
// @match     http*://*/auth-login-stub/gg-sign-in?continue=*plastic-packaging-tax*
// @grant     none
// @updateURL https://raw.githubusercontent.com/hmrc/plastic-packaging-tax-frontend/master/tampermonkey/PPT_Auth_AutoComplete.js
// ==/UserScript==

(function() {
    'use strict';

    document.getElementsByName("redirectionUrl")[0].value = getBaseUrl() + "/plastic-packaging-tax/start-plastic-packaging-tax-registration";

    document.getElementById("affinityGroupSelect").selectedIndex = 1;

    document.getElementsByName("enrolment[0].name")[0].value = "HMRC-PPT-ORG";
    document.getElementById("input-0-0-name").value = "PPTReference";
    document.getElementById("input-0-0-value").value = "XMPPT0000000001";

    document.getElementById('global-header').appendChild(createQuickButton())

})();

function createQuickButton() {
    let button = document.createElement('button');
    button.id='quickSubmit'

    if (!!document.getElementById('global-header')) {
        button.classList.add('button-start', 'govuk-!-display-none-print')
    } else {
        button.classList.add('govuk-button','govuk-!-display-none-print')
    }

    button.style.position = 'absolute'
    button.style.top = '50px'
    button.innerHTML = 'Quick Submit'
    button.onclick = () => document.getElementsByClassName('button')[0].click();
    return button;

}

function getBaseUrl() {
    let host = window.location.host;
    if (window.location.hostname === 'localhost') {
        host = 'localhost:8503'
    }
    return window.location.protocol + "//" + host;
}
