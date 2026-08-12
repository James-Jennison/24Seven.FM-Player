(function () {
  'use strict';

  var all = document.getElementById('select-all');
  var editor = document.getElementById('body-editor');
  var hidden = document.getElementById('body-html');
  var form = document.getElementById('email-form');
  if (!all || !editor || !hidden || !form) return;

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
