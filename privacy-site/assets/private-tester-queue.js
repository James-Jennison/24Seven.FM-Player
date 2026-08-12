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
      subject: 'Welcome to the 24Seven.FM Player Closed Alpha test',
      html: '<p>Hi,</p>' +
        '<p>Welcome to the internal test for 24Seven.FM Player. Thank you for helping us validate the app before wider release.</p>' +
        '<p><strong>Join on Android:</strong><br><a href="https://play.google.com/store/apps/details?id=com.codeframe78.twentyfourseven.player">https://play.google.com/store/apps/details?id=com.codeframe78.twentyfourseven.player</a></p>' +
        '<p><strong>Join on the web:</strong><br><a href="https://play.google.com/apps/testing/com.codeframe78.twentyfourseven.player">https://play.google.com/apps/testing/com.codeframe78.twentyfourseven.player</a></p>' +
        '<p>Open the link while signed in to Google Play with the Google account added to the tester list. Updates will arrive through Google Play during the test.</p>' +
        '<h3>Quick start</h3><ol><li>Open 24Seven.FM Player and allow notifications if prompted.</li><li>On the Player screen, swipe across the station cards to select a station.</li><li>Tap the central Play button to begin live radio.</li><li>Confirm the station name, artwork, and current track appear.</li><li>Background the app briefly; playback should continue with Android media controls.</li><li>Tap the media notification to return to the Player, then try Pause/Play and another station.</li></ol>' +
        '<h3>Signing in to a station</h3><ol><li>Select the station you want to use.</li><li>Tap the account and station options button in the upper-right corner of the app.</li><li>Choose the station’s sign-in option. This opens that station’s own login page.</li><li>Sign in there with your existing account for that specific station, then return to the app.</li><li>Confirm the app shows that station as signed in.</li></ol>' +
        '<p>Each station uses its own website account. Signing in to one station does not sign you in to the others. Available features—such as Favorites, Chat participation, requests, and account information—depend on the selected station and its supported capabilities.</p>' +
        '<p><strong>Please do not include passwords, CAPTCHA answers, session information, or screenshots containing credentials in feedback.</strong></p>' +
        '<h3>Testing matrix</h3><p><a href="https://player.jamesjennison.net/product-testing/">https://player.jamesjennison.net/product-testing/</a></p><p>Use the matrix to choose a test case or a normal listening scenario suitable for your device.</p>' +
        '<h3>Submit feedback</h3><p><a href="https://github.com/James-Jennison/24Seven.FM-Player/issues/new?template=product-test.yml">https://github.com/James-Jennison/24Seven.FM-Player/issues/new?template=product-test.yml</a></p><p>Please report passes, issues, usability notes, and blocked tests. Include the app version, device and Android version, station selected, steps taken, expected versus actual behavior, and whether it repeats.</p>' +
        '<p>GitHub issues are public. Do not include credentials, private messages, cookies, session details, full network captures, or unredacted personal screenshots.</p>' +
        '<p>For in-app help, use <strong>More → Contact Us</strong>. Privacy information: <a href="https://player.jamesjennison.net/privacy/">https://player.jamesjennison.net/privacy/</a></p>' +
        '<p>Thanks again for helping shape 24Seven.FM Player.</p><p>—James</p>'
    },
    feedback: {
      subject: '24Seven.FM Player Closed Alpha: detailed testing feedback request',
      html: '<p>Hi,</p>' +
        '<p>Thank you for taking part in the 24Seven.FM Player Closed Alpha. Your detailed feedback helps us find issues and improve the next build.</p>' +
        '<h3>What to test</h3><p>Use the app normally as well as trying the areas below:</p><ul><li>Start and pause live playback, then switch between stations.</li><li>Check that station artwork, station name, and current-track information update correctly.</li><li>Briefly background the app and confirm playback and Android media controls behave as expected.</li><li>Return from the media notification and try Play/Pause plus another station.</li><li>If available for the selected station, try its sign-in flow and confirm the app reflects the sign-in state after you return.</li></ul>' +
        '<h3>How to send feedback</h3><p>Use the feedback form: <a href="https://github.com/James-Jennison/24Seven.FM-Player/issues/new?template=product-test.yml">https://github.com/James-Jennison/24Seven.FM-Player/issues/new?template=product-test.yml</a></p><p>For every issue, please include:</p><ol><li>A short, clear title.</li><li>Your app version, phone or tablet model, and Android version.</li><li>The station selected and whether you were signed in.</li><li>The exact steps you took.</li><li>What you expected to happen and what happened instead.</li><li>Whether the issue repeats and how often.</li><li>A screenshot or screen recording when helpful, with personal information removed.</li></ol>' +
        '<h3>Useful feedback even when nothing breaks</h3><p>Please also share successful tests, confusing wording, slow or awkward interactions, missing information, or anything that would make the Player easier to use.</p>' +
        '<p>You can use the testing matrix to choose a focused test case: <a href="https://player.jamesjennison.net/product-testing/">https://player.jamesjennison.net/product-testing/</a></p>' +
        '<p><strong>Privacy reminder:</strong> GitHub issues are public. Do not include passwords, CAPTCHA answers, private messages, cookies, session details, full network captures, or unredacted personal screenshots.</p>' +
        '<p>Thanks again for helping shape 24Seven.FM Player.</p><p>—James</p>'
    },
    'new-build': {
      subject: '24Seven.FM Player Closed Alpha: new build available [version]',
      html: '<p>Hi,</p><p>A new 24Seven.FM Player Closed Alpha build is ready to test.</p><h3>What changed</h3><ul><li>[Change or fix 1]</li><li>[Change or fix 2]</li><li>[Change or fix 3]</li></ul><h3>What to focus on</h3><p>[Describe the feature, station, device scenario, or issue that needs attention.]</p><p>Update through Google Play, then send feedback through <a href="https://github.com/James-Jennison/24Seven.FM-Player/issues/new?template=product-test.yml">the feedback form</a>. Please include the build version, device, Android version, and steps taken.</p><p>Thanks,<br>24Seven.FM Player</p>'
    },
    reminder: {
      subject: 'Reminder: 24Seven.FM Player Closed Alpha testing',
      html: '<p>Hi,</p><p>A quick reminder that the 24Seven.FM Player Closed Alpha is still open for testing.</p><p>If you have a few minutes, please update the app, try normal listening plus station switching and background playback, and let us know how it goes.</p><p>Send feedback by [date] using <a href="https://github.com/James-Jennison/24Seven.FM-Player/issues/new?template=product-test.yml">the feedback form</a>. Successful tests and usability notes are just as useful as bug reports.</p><p>Thanks for your help,<br>24Seven.FM Player</p>'
    },
    'known-issue': {
      subject: '24Seven.FM Player Closed Alpha: known issue and workaround',
      html: '<p>Hi,</p><p>We are aware of the following issue in the current Closed Alpha build:</p><h3>[Known issue]</h3><p>[Describe what testers may see, which devices or stations are affected, and when it happens.]</p><h3>Workaround</h3><ol><li>[Workaround step 1]</li><li>[Workaround step 2]</li></ol><p>Please avoid filing duplicate reports for this issue unless you have a different cause, device-specific detail, or a way to reproduce it consistently.</p><p>We will share an update when a fix is available. Thanks,<br>24Seven.FM Player</p>'
    },
    'test-session': {
      subject: '24Seven.FM Player Closed Alpha: focused test session request',
      html: '<p>Hi,</p><p>We would appreciate your help with a focused testing session.</p><h3>Session details</h3><ul><li><strong>When:</strong> [date and time]</li><li><strong>Focus:</strong> [feature, station, device type, or scenario]</li><li><strong>Goal:</strong> [what you want to confirm]</li></ul><h3>Please try</h3><ol><li>[Test action 1]</li><li>[Test action 2]</li><li>[Test action 3]</li></ol><p>Report results through <a href="https://github.com/James-Jennison/24Seven.FM-Player/issues/new?template=product-test.yml">the feedback form</a>, including a pass result if everything works as expected.</p><p>Thanks,<br>24Seven.FM Player</p>'
    },
    'test-complete': {
      subject: 'Thank you for testing 24Seven.FM Player Closed Alpha',
      html: '<p>Hi,</p><p>Thank you for your time and feedback during this Closed Alpha testing period.</p><p>Your reports, successful test results, and usability notes have helped shape the next stage of 24Seven.FM Player.</p><p>[Add any next-step information, such as the next build, testing pause, or release timing.]</p><p>Thanks again,<br>24Seven.FM Player</p>'
    },
    'tester-status': {
      subject: '24Seven.FM Player Closed Alpha: tester status update',
      html: '<p>Hi,</p><p>This is an update about your 24Seven.FM Player Closed Alpha tester status.</p><p>[Add the status change or update here, including any action the tester should take.]</p><p>If you have questions, reply to this email.</p><p>Thanks,<br>24Seven.FM Player</p>'
    },
    'release-signoff': {
      subject: '24Seven.FM Player: release candidate sign-off request',
      html: '<p>Hi,</p><p>We are preparing a release candidate and would appreciate a final sign-off from Closed Alpha testers.</p><h3>Please confirm</h3><ol><li>Whether you can install and open the current build.</li><li>Whether live playback, station switching, and background media controls work on your device.</li><li>Whether you have any release-blocking issues.</li></ol><p>Please reply by [date] with one of these outcomes: <strong>Works as expected</strong>, <strong>Issue found</strong>, or <strong>Blocked</strong>. For an issue or block, include your device, Android version, app version, and steps to reproduce.</p><p>Thanks for helping us reach a confident release decision,<br>24Seven.FM Player</p>'
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
