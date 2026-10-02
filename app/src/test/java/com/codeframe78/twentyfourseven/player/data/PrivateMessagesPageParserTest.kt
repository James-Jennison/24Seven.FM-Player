package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.PrivateMessageFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateMessagesPageParserTest {
    private val parser = PrivateMessagesPageParser()
    private val origin = "https://streamingsoundtracks.com/"

    @Test
    fun `reads the folder rows, unread marker, pages, and unread count`() {
        val page = parser.parseFolder(PrivateMessageFixtures.folderPage(), origin)

        assertEquals(listOf("901", "877"), page.messages.map { it.id })
        with(page.messages.first()) {
            assertEquals("Player delivery test", subject)
            assertEquals("Listener", correspondent)
            assertEquals("Oct 02, 2026", dateLabel)
            assertTrue(isUnread)
        }
        assertFalse(page.messages.last().isUnread)
        assertEquals(1, page.page)
        assertEquals(3, page.pageCount)
        assertEquals(1, page.unreadCount)
    }

    @Test
    fun `ignores message links that point at another site`() {
        val page = parser.parseFolder(
            PrivateMessageFixtures.folderPage(
                firstLink = "https://example.com/modules.php?name=Private_Messages&amp;folder=inbox&amp;mode=read&amp;id=901",
            ),
            origin,
        )

        assertEquals(listOf("877"), page.messages.map { it.id })
    }

    @Test
    fun `reads a message with its line breaks and reply action`() {
        val message = parser.parseMessage(PrivateMessageFixtures.MESSAGE_PAGE, origin, PrivateMessageFolder.Inbox, "901")

        assertEquals("Listener", message.sender)
        assertEquals("Member", message.recipient)
        assertEquals("October 02, 2026 07:02 am", message.dateLabel)
        assertEquals("Player delivery test", message.subject)
        assertEquals("First line café\n\n[quote]\nquoted line\n[/quote]", message.body)
        assertTrue(message.canReply)
    }

    @Test
    fun `reads the station's own form for a reply`() {
        val form = parser.parseForm(PrivateMessageFixtures.formPage(), origin)

        assertEquals("Listener", form?.recipient)
        assertEquals("Re: Player delivery test", form?.subject)
        assertEquals("\n\n\n[quote]\nFirst line\n[/quote]", form?.body)
        assertEquals(listOf("u" to "57", "op" to "sendmsg"), form?.hiddenFields)
        assertEquals("1", form?.saveCopyValue)
    }

    @Test
    fun `a page without a complete message form yields no form`() {
        assertNull(parser.parseForm(PrivateMessageFixtures.folderPage(), origin))
        assertNull(parser.parseForm(PrivateMessageFixtures.formPage(memberNumber = ""), origin))
    }

    @Test
    fun `finds the member number a profile offers for a private message`() {
        val profile = """
            <html><body>
              <a href="modules.php?name=Private_Messages&amp;file=index&amp;mode=post&amp;u=4821"><img src="icon_pm.gif"></a>
              <a href="modules.php?name=Forums&amp;file=profile&amp;mode=email&amp;u=4821">Email</a>
            </body></html>
        """.trimIndent()

        assertEquals("4821", parser.parseProfileMemberNumber(profile, origin))
        assertNull(parser.parseProfileMemberNumber("<html><body>No profile</body></html>", origin))
    }
}

internal object PrivateMessageFixtures {
    fun folderPage(
        firstLink: String = "modules.php?name=Private_Messages&amp;folder=inbox&amp;mode=read&amp;id=901",
        firstSubject: String = "Player delivery test",
        firstCorrespondent: String = "Listener",
    ) = """
        <html><body>
          <a href="/modules.php?name=Your_Account"><span id="auth-dot">x</span> Member <span class="pm-badge" data-pm-count>1</span></a>
          <ul><li><a href="/modules.php?name=Your_Account&amp;op=logout">Logout</a></li></ul>
          <table><tr><td>
            <form action="modules.php?name=Private_Messages" method="post"><b>Folder:</b>
              <select name="folder"><option value="inbox" selected>Inbox</option><option value="outbox">Sent Box</option></select>
            </form>
            Inbox: <b>41</b> - Sent Box: <b>12</b> - Save Box: <b>0</b>
            [ <a href="modules.php?name=Private_Messages&amp;mode=post">New Message</a> ]
          </td></tr></table>
          <table>
            <tr><td><b>!</b></td><td><b>Subject</b></td><td><b>From</b></td><td><b>Date</b></td><td><b>Smile</b></td></tr>
            <tr>
              <td><img src="/images/folder_new.gif"></td>
              <td><a href="$firstLink">$firstSubject</a></td>
              <td><a href="modules.php?name=Your_Account&amp;op=userinfo&amp;username=$firstCorrespondent">$firstCorrespondent</a></td>
              <td><font>Oct 02, 2026</font></td>
              <td><input type="checkbox" name="msg_id[]" value="901"></td>
            </tr>
            <tr>
              <td><img src="/images/folder.gif"></td>
              <td><a href="modules.php?name=Private_Messages&amp;folder=inbox&amp;mode=read&amp;id=877">Re: Queue</a></td>
              <td><a href="modules.php?name=Your_Account&amp;op=userinfo&amp;username=Other">Other</a></td>
              <td><font>Sep 30, 2026</font></td>
              <td><input type="checkbox" name="msg_id[]" value="877"></td>
            </tr>
            <tr><td colspan="5"><br>Messages: <b>41</b> - Page: <b>[1]</b>
              <a href="modules.php?name=Private_Messages&amp;folder=inbox&amp;p=2">2</a>
              <a href="modules.php?name=Private_Messages&amp;folder=inbox&amp;p=3">3</a>
            </td></tr>
          </table>
        </body></html>
    """.trimIndent()

    const val MESSAGE_PAGE = """
        <html><body>
          <ul><li><a href="/modules.php?name=Your_Account&amp;op=logout">Logout</a></li></ul>
          <table border="0" align="center">
            <tr><td><b>From:</b></td><td>Listener</td></tr>
            <tr><td><b>To:</b></td><td>Member</td></tr>
            <tr><td><b>Date:</b></td><td>October 02, 2026 07:02 am</td></tr>
            <tr><td><b>Subject:</b></td><td>Player delivery test</td></tr>
            <tr><td colspan="2"><br>First line caf&eacute;<br><br> [quote]<br> quoted line<br> [/quote]<br><br></td></tr>
            <tr><td colspan="2">[ <a href="modules.php?name=Private_Messages&amp;mode=post&amp;reply=1&amp;id=901">Reply</a>
              | <a href="modules.php?name=Private_Messages&amp;op=savemsg&amp;id=901">Save</a>
              | <a href="modules.php?name=Private_Messages&amp;op=delmsg&amp;id=901">Delete</a> ]</td></tr>
          </table>
        </body></html>
    """

    fun formPage(recipient: String = "Listener", memberNumber: String = "57") = """
        <html><body>
          <ul><li><a href="/modules.php?name=Your_Account&amp;op=logout">Logout</a></li></ul>
          <table border="0" align="center"><form action="modules.php?name=Private_Messages" method="post">
            <tr><td><b>To:</b></td><td>$recipient</td></tr>
            <tr><td><b>Subject:</b></td><td><input type="text" name="subject" maxlength="100" value="Re: Player delivery test"></td></tr>
            <tr><td colspan="2"><br><textarea name="message" rows="10" cols="55">


[quote]
First line
[/quote]</textarea><br><br></td></tr>
            <tr><td colspan="2"><input type="checkbox" name="savecopy" value="1"> Save a Copy<br><br>
              <input type="hidden" name="u" value="$memberNumber"><input type="hidden" name="op" value="sendmsg">
              <input type="submit" value="Send"></td></tr>
          </form></table>
        </body></html>
    """.trimIndent()
}
