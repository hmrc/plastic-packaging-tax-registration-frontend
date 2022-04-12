// ==UserScript==
// @name     Plastic Packaging Tax Registration Authorisation
// @namespace  http://tampermonkey.net/
// @version   6.6
// @description Auth Wizard autocomplete script for PPT
// @author    pmonteiro
// @match     http*://*/auth-login-stub/gg-sign-in?continue=*register-for-plastic-packaging-tax*
// @grant     none
// @updateURL https://raw.githubusercontent.com/hmrc/plastic-packaging-tax-frontend/master/tampermonkey/PPT_Auth_AutoComplete.js
// ==/UserScript==

(function() {
    'use strict';

    document.getElementsByName("redirectionUrl")[0].value = getBaseUrl() + "/register-for-plastic-packaging-tax/start";

    document.getElementById("affinityGroupSelect").selectedIndex = 1;

    document.querySelector('header').appendChild(pptPanel())

})();

function pptPanel() {
    var panel = document.createElement("div");

    panel.appendChild(createQuickButton());

    return panel;
}

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
    button.onclick = () => document.getElementById('submit').click();
    return button;

}

function getBaseUrl() {
    let host = window.location.host;
    if (window.location.hostname === 'localhost') {
        host = 'localhost:8503'
    }
    return window.location.protocol + "//" + host;
}
