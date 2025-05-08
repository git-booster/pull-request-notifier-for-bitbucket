
function prNotifierButtonClicked(event) {
 var v = document.getElementById('prNotifierMenu');
 var a = v.getAttribute('aria-expanded');
 if (a === 'true') {
  return prNotifierCollapse(event);
 } else {
  v.setAttribute('aria-expanded', 'true');
  return false;
 }
}

function prNotifierCollapse(event) {
 event.preventDefault();
 event.stopPropagation();
 var a = document.getElementById('prNotifierActualButton');
 var v = document.getElementById('prNotifierMenu');
 a.blur();
 v.setAttribute('aria-expanded', 'false');
 return false;
}

window.addEventListener("load", function load(event) {
 window.removeEventListener("load", load);

 define('plugin/prnfb/pr-triggerbutton', [
  'jquery',
  '@atlassian/aui',
  'bitbucket/util/navbuilder',
  'bitbucket/util/state',
  'underscore',
  'plugin/prnfb/3rdparty'
 ], function ($, AJS, NAV, pageState, _, thirdParty) {
  var buttonsAdminUrl = NAV.rest().build() + "/../../prnfb-admin/1.0/settings/buttons";

  var waiting = '<span class="aui-icon aui-icon-wait aui-icon-small">Wait</span>';

  var $mergeButton = $('.merge-button');
  var hasMergeButton = $mergeButton && $mergeButton.length && $mergeButton.length > 0;
  if (!hasMergeButton) {
   $mergeButton = $('.edit-pull-request');
  }
  hasMergeButton = $mergeButton && $mergeButton.length && $mergeButton.length > 0;
  if (hasMergeButton) {
   $mergeButton = $($mergeButton[0]);

   var classList = $mergeButton[0].classList;
   var cssClass = '';
   for(var i = 0; i < classList.length; i++){
     var ii = classList[i];
     if (ii.startsWith("css-")) {
       cssClass = ii;
       break;
     }
   }

   $mergeButton.after(" &nbsp;<div id='prNotifierButton' style='display:none;'><button id='prNotifierActualButton' class='" + cssClass + "' tabindex='0' onclick='return prNotifierButtonClicked(event);' onblur='return prNotifierCollapse(event);'>PR-Notifier&nbsp;Buttons</button>" +
       "<div id='prNotifierMenu' aria-expanded='false'><ul id='prNotifierUL'></ul></div>" +
       "</div>&nbsp; ");
  }
  var $buttonArea = $('#prNotifierUL');

  var buttonTemplate = function (name) {
   return $('<li><button class="prnfb-button">' + name + '</button></li>');
  };

  var dialogTemplate = function (name, content) {
   var escapedName = _.escape(name);
   return '<section role="dialog" id="confirm-dialog" class="aui-layer aui-dialog2 aui-dialog2-medium"' + //
       ' data-aui-remove-on-hide="true" aria-hidden="true">' + //
       '    <!-- Dialog header -->' + //
       '    <header class="aui-dialog2-header">' + //
       '        <h2 class="aui-dialog2-header-main">' + escapedName + '</h2>' + //
       '        <!-- Close icon -->' + //
       '        <a class="aui-dialog2-header-close">' + //
       '            <span class="aui-icon aui-icon-small aui-iconfont-close-dialog">Close</span>' + //
       '        </a>' + //
       '    </header>' + //
       '    <!-- Main dialog content -->' + //
       '    <div class="aui-dialog2-content">' + //
       content + //
       '    </div>' + //
       '    <!-- Dialog footer -->' + //
       '    <footer class="aui-dialog2-footer">' + //
       '        <!-- Actions to render on the right of the footer -->' + //
       '        <div class="aui-dialog2-footer-actions">' + //
       '            <button id="dialog-submit-button" class="aui-button aui-button-primary">' + name + '</button>' + //
       '            <button id="dialog-close-button" class="aui-button aui-button-link">Close</button>' + //
       '        </div>' + //
       '    </footer>' + //
       '</section>';
  };

  var inputTemplate = function (name, item) {
   var escapedName = _.escape(name);
   var escapedLabel = _.escape(item.label);
   var escapedValue = _.escape(item.defaultValue);
   var escapedDescription = _.escape(item.description);
   return $('' +
       '<div class="field-group">' + //
       '<label for="' + name + '">' + escapedLabel + //
       (!item.required ? '' : '<span class="aui-icon icon-required">(required)</span>') + '</label>' + //
       '<input class="text medium-field" type="text" ' + //
       'id="prnfb-form-' + name + '" name="' + name + '" placeholder="' + escapedValue + '" value="' + escapedValue + '">' + //
       '<div class="description">' + escapedDescription + '</div>' + //
       '</div>');
  };

  var textareaTemplate = function (name, item) {
   var escapedName = _.escape(name);
   var escapedLabel = _.escape(item.label);
   var escapedValue = _.escape(item.defaultValue);
   var escapedDescription = _.escape(item.description);
   return $('' +
       '<div class="field-group">' + //
       '<label for="' + escapedName + '">' + escapedLabel + //
       (!item.required ? '' : '<span class="aui-icon icon-required">(required)</span>') + '</label>' + //
       '<textarea class="textarea" id="prnfb-form-' + name + '" name="' + name + '" placeholder="' + escapedValue + '">' + //
       escapedValue + '</textarea>' + //
       '<div class="description">' + escapedDescription + '</div>' + //
       '</div>');
  };

  var checkboxTemplate = function (name, item) {
   var checkboxItemTemplate = function (nestedItem, num) {
    var isChecked = nestedItem.defaultValue;
    var outerName = _.escape(name);
    var itemName = _.escape(nestedItem.name);
    var itemLabel = _.escape(nestedItem.label);
    return $('<div class="checkbox">' + //
        '<input class="checkbox" value="' + itemName + '" type="checkbox" ' + (isChecked ? 'checked="checked"' : '') + //
        ' name="' + outerName + '[]" id="prnfb-form-' + outerName + '-' + num + '">' + //
        '<label for="prnfb-form-' + outerName + '-' + num + '"' + //
        '>' + itemLabel + '</label></div>');
   };

   var escapedLabel = _.escape(item.label);
   var escapedDescription = _.escape(item.description);
   var checkboxItems = [$('<legend><span>' + escapedLabel + '</span></legend>')];
   for (var i = 0; i < item.buttonFormElementOptionList.length; i++) {
    checkboxItems.push(checkboxItemTemplate(item.buttonFormElementOptionList[i], i));
   }
   checkboxItems.push($('<div class="description">' + escapedDescription + '</div>'));
   return $("<fieldset class='group'/>").append(checkboxItems);
  };

  var radioTemplate = function (name, item) {
   var radioItemTemplate = function (nestedItem, num) {
    var isChecked = nestedItem.name === item.defaultValue;
    var outerName = _.escape(name);
    var itemName = _.escape(nestedItem.name);
    var itemLabel = _.escape(nestedItem.label);
    return $('<div class="radio">' + //
        '<input class="radio" value="' + itemName + '" type="radio" ' + (isChecked ? 'checked="checked"' : '') + //
        'name="' + outerName + '" id="prnfb-form-' + outerName + '-' + num + '">' + //
        '<label for="prnfb-form-' + outerName + '-' + num + '">' + itemLabel + '</label></div>');
   };

   var escapedLabel = _.escape(item.label);
   var escapedDescription = _.escape(item.description);
   var radioItems = [$('<legend><span>' + escapedLabel + '</span></legend>')];
   for (var i = 0; i < item.buttonFormElementOptionList.length; i++) {
    radioItems.push(radioItemTemplate(item.buttonFormElementOptionList[i], i));
   }
   radioItems.push($('<div class="description">' + escapedDescription + '</div>'));
   return $("<fieldset class='group'/>").append(radioItems);
  };

  var confirmationTextTemplate = function (confirmationText) {
   if (!confirmationText) {
    return '';
   }

   var confirmationDiv = '<div class="description">' + confirmationText + '</div>';
   return confirmationDiv;
  };

  var formTemplate = function (formDescription) {
   if (!formDescription || formDescription.length === 0) {
    return '';
   }

   var formItems = [];
   for (var i = 0; i < formDescription.length; i++) {
    var item = formDescription[i];
    switch (item.type) {
     case "input": {
      formItems.push(inputTemplate(item.name, item));
      break;
     }
     case "textarea": {
      formItems.push(textareaTemplate(item.name, item));
      break;
     }
     case "checkbox": {
      formItems.push(checkboxTemplate(item.name, item));
      break;
     }
     case "radio": {
      formItems.push(radioTemplate(item.name, item));
      break;
     }
    }
   }
   var form = $("<form style='display:block;' class='aui'></form>");
   form.append(formItems);

   return form;
  };

  var presentResult = function (response) {
   var successTriggers = [];
   var failTriggers = [];
   if (response) {
    for (var i = 0; i < response.length; i++) {
     var notificationResponse = response[i];
     if (notificationResponse.status >= 200 && notificationResponse.status <= 299) {
      AJS.flag({
       close: 'auto',
       type: 'success',
       title: notificationResponse.notificationName.replace(/<script>/g, 'script'),
       body: '<p>You may check network tab in web browser for exact URL and response.</p>'
      });
     } else {
      AJS.flag({
       close: 'auto',
       type: 'error',
       title: notificationResponse.notificationName.replace(/<script>/g, 'script'),
       body: '<p>' + notificationResponse.status + ' ' + notificationResponse.uri + '</p>' +
           '<p>You may check network tab in web browser for exact URL and response.</p>'
      });
     }
    }
   }

   if (!response || response.length === 0) {
    AJS.flag({
     close: 'auto',
     type: 'warning',
     title: 'No triggers were invoked',
     body: '<p>No triggers were invoked when buttons was pressed.</p>'
    });
   }
  };

  function loadSettingsAndShowButtons() {

   $.get(buttonsAdminUrl + '/fromPR' + window.location.pathname, function (settings) {
    var hasButtons = false;
    settings.forEach(function (item) {
     hasButtons = true;
     var $buttonDropdownItem = buttonTemplate(item.name.replace(/<script>/g, 'script'));
     $buttonDropdownItem[0].onmousedown = function (event) {
      event.preventDefault();
      event.stopPropagation();

      var $this = $(this);

      var enableButton = function () {
       $this.removeAttr("disabled");
       $this.removeAttr("aria-disabled");
       $this.find("span").remove();
      };
      var disableButton = function () {
       $this.attr("disabled", "disabled");
       $this.attr("aria-disabled", "true");
       $this.prepend(waiting);
      };

      var submitButton = function (formResult) {
       disableButton();
       $.ajax({
        "type": "POST",
        "url": buttonsAdminUrl + '/fromUUID' + window.location.pathname + '/uuid/' + item.uuid,
        "data": {
         "form": formResult
        },
        "success": function (content) {
         setTimeout(function () {
          enableButton();
          if (content.confirmation === "on") {
           presentResult(content.notificationResponses);
          }
         }, 500);
        },
        "error": function (content) {
         enableButton();
         AJS.flag({
          close: 'auto',
          type: 'error',
          title: "Unknown error",
          body: '<p>' + content.status + '</p>' + '<p>Check the Bitbucket Server log for more details.</p>'
         });
        }
       });

       if (item.redirectUrl) {
        redirect();
       }
      };

      var redirect = function () {
       disableButton();
       window.location.replace(item.redirectUrl);
      };

      if (item.confirmationText || item.buttonFormList && item.buttonFormList.length > 0) {
       // Create the form and dialog
       var confirmationText = confirmationTextTemplate(item.confirmationText);
       var form = formTemplate(item.buttonFormList);
       var formHtml = $("<div/>").append(confirmationText).append(form).html();
       var $dialog = $(dialogTemplate(item.name, formHtml));
       $dialog.appendTo($("body"));

       var dialogRef = AJS.dialog2($dialog);

       // When you submit the form, we will post to the server with all the
       // form data.
       AJS.$("#dialog-submit-button").click(function (e) {
        var formResult = $dialog.find("form").serializeJSON();
        e.preventDefault();
        dialogRef.hide();

        submitButton(formResult);
       });
       AJS.$("#dialog-close-button").click(function (e) {
        e.preventDefault();
        dialogRef.hide();
       });
       dialogRef.show();
      } else {
       submitButton(null);
      }

     };

     $buttonArea.append($buttonDropdownItem);

    });

    if (hasButtons) {
     // Okay, we really have some PR-Notifier buttons to show!  Let's show them!
     $('#prNotifierButton').removeAttr('style').css("display", "block").css("float", "right");
    }

   });
  }

  loadSettingsAndShowButtons();

  //If a reviewer approves the PR, then a button may become visible
  $('.aui-button.approve').click(function () {
   setTimeout(function () {
    loadSettingsAndShowButtons();
   }, 1000);
  });
 });

 AJS.$(document).ready(function () {
  require('plugin/prnfb/pr-triggerbutton');
 });

});
