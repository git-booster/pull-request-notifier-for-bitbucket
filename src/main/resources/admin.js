define('plugin/prnfb/admin', [
 'jquery',
 '@atlassian/aui',
 'bitbucket/util/navbuilder',
 'plugin/prnfb/utils'
], function($, AJS, NAV, utils) {
 var settingsAdminUrlPostUrl = NAV.rest().build() + "/../../prnfb-admin/1.0/settings";
 var settingsAdminUrl = settingsAdminUrlPostUrl;

 var notificationsAdminUrlPostUrl = NAV.rest().build() + "/../../prnfb-admin/1.0/settings/notifications";
 var notificationsAdminUrl = notificationsAdminUrlPostUrl;

 var buttonsAdminUrlPostUrl = NAV.rest().build() + "/../../prnfb-admin/1.0/settings/buttons";
 var buttonsAdminUrl = buttonsAdminUrlPostUrl;

 var projectKey;
 if ($('#prnfbRepositorySlug').length !== 0) {
  projectKey = $('#prnfbProjectKey').val();
  var repositorySlug = $('#prnfbRepositorySlug').val();

  notificationsAdminUrl = notificationsAdminUrlPostUrl + '/projects/' + projectKey + '/repos/' + repositorySlug;
  buttonsAdminUrl = buttonsAdminUrlPostUrl + '/projects/' + projectKey + '/repos/' + repositorySlug;
 } else if ($('#prnfbProjectKey').length !== 0) {
  projectKey = $('#prnfbProjectKey').val();

  notificationsAdminUrl = notificationsAdminUrlPostUrl + '/projects/' + projectKey;
  buttonsAdminUrl = buttonsAdminUrlPostUrl + '/projects/' + projectKey;
 }

 $(document)
  .ajaxStart(function() {
   $('.prnfb button').attr('aria-disabled', 'true');
  })
  .ajaxStop(function() {
   $('.prnfb button').attr('aria-disabled', 'false');
  });

 function disableOrDelete() {
  var buttonText = $(this).text();
  var uuid = this.value;
  var action = "unset";
  var doAction = false;
  var doDelete = false;
  if (buttonText === "DELETE") {
   action = "";
   doAction = true;
   doDelete = true;
  } else if (buttonText === "ENABLE") {
   action = "/enable";
   doAction = true;
  } else if (buttonText === "DISABLE") {
   action = "/disable";
   doAction = true;
  }

  if (doAction) {
   $.ajax({
    url: notificationsAdminUrlPostUrl + action + '/' + uuid,
    type: doDelete ? 'DELETE' : 'GET',
    success: function (result) {
     window.location.reload();
    }
   });
  }
 }

 $(document).ready(function() {
  utils.setupForm('#prnfbsettingsadmin', settingsAdminUrl, settingsAdminUrlPostUrl);
  utils.setupForms('#prnfbbuttonadmin', buttonsAdminUrl, buttonsAdminUrlPostUrl);
  utils.setupForms('#prnfbnotificationadmin', notificationsAdminUrl, notificationsAdminUrlPostUrl);

  $('#prNotifierConfigReport button').click(disableOrDelete);

 });

});


if (AJS && AJS.$) {
 AJS.$(document).ready(function() {
  require('plugin/prnfb/admin');
 });
} else {
 $(document).ready(function() {
  require('plugin/prnfb/admin');
 });
}
