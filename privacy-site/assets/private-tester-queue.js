(function () {
  'use strict';

  var all = document.getElementById('select-all');
  var editor = document.getElementById('body-editor');
  var hidden = document.getElementById('body-html');
  var form = document.getElementById('email-form');
  var template = document.getElementById('email-template');
  var subject = document.getElementById('subject');
  if (!all || !editor || !hidden || !form || !template || !subject) return;

  var templates = {
    welcome: {
      subject: 'Welcome to the 24Seven.FM Player internal test',
      html: '<p>Hello,</p><p>Thank you for joining the 24Seven.FM Player internal test.</p><p>We will share testing instructions and the latest build details with you shortly.</p><p>Please reply to this email if you have any questions.</p><p>Thanks,<br>24Seven.FM Player</p>'
    },
    feedback: {
      subject: '24Seven.FM Player testing feedback request',
      html: '<p>Hello,</p><p>Thank you for testing the 24Seven.FM Player.</p><p>When you have a moment, please share any issues you found, what you expected to happen, and the device you used.</p><p>Your feedback helps us improve the next build.</p><p>Thanks,<br>24Seven.FM Player</p>'
    }
  };

  var savedRange = null;

  function saveRange() {
    var selection = window.getSelection();
    if (!selection || !selection.rangeCount || !editor.contains(selection.anchorNode)) return;
    savedRange = selection.getRangeAt(0).cloneRange();
  }

  function restoreRange() {
    if (!savedRange) {
      editor.focus();
      return;
    }
    var selection = window.getSelection();
    selection.removeAllRanges();
    selection.addRange(savedRange);
  }

  all.addEventListener('change', function () {
    document.querySelectorAll('input[name="tester_ids[]"]').forEach(function (box) {
      box.checked = all.checked;
    });
  });

  template.addEventListener('change', function () {
    var selected = templates[template.value];
    if (!selected) return;
    subject.value = selected.subject;
    editor.innerHTML = selected.html;
    hidden.value = selected.html;
    editor.focus();
    saveRange();
  });

  editor.addEventListener('keyup', saveRange);
  editor.addEventListener('mouseup', saveRange);
  editor.addEventListener('input', saveRange);
  document.addEventListener('selectionchange', saveRange);

  document.querySelectorAll('[data-format]').forEach(function (button) {
    button.addEventListener('mousedown', function (event) {
      event.preventDefault();
    });
    button.addEventListener('click', function () {
      var command = button.dataset.format;
      if (command === 'createLink') {
        var url = window.prompt('Link URL (http, https, or mailto):');
        if (!url) return;
        restoreRange();
        document.execCommand(command, false, url);
      } else {
        restoreRange();
        document.execCommand(command, false, null);
      }
      saveRange();
    });
  });

  form.addEventListener('submit', function (event) {
    hidden.value = editor.innerHTML;
    if (editor.textContent.trim()) return;
    event.preventDefault();
    editor.focus();
    window.alert('Write an email message before reviewing recipients.');
  });
}());
