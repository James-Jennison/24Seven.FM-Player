package com.codeframe78.twentyfourseven.player.data

/** Compact copies of the stations' community pages, with invented identities. */
internal object StationCommunityFixtures {
    private const val PROFILE = "/modules.php?name=Forums&file=profile&mode=viewprofile"

    fun onlineBlock(listenerHref: String = "$PROFILE&u=101") = """
        <html><body><table><tr><td>
        <b>News</b> Published by <a href="$PROFILE&username=Editor">Editor</a> and <a href="$PROFILE&u=999">Decoy</a><hr>
        <img src="images/blocks/group.gif" alt="Group"> <b><u>Membership:</u></b><br>
        <img src="images/blocks/member.gif" alt="Member"> Latest: <a href="/modules.php?name=Your_Account&amp;op=userinfo&amp;username=Newest"><b>Newest</b></a><br>
        <img src="images/blocks/member.gif" alt="Member"> Overall: <b>12,345</b><br>
        <hr>
        <img src="images/blocks/group.gif" alt="Group"> <b><u>People Online:</u></b> <br>
        <img src="images/blocks/member.gif" alt="Member"> Visitors: <b>40</b><br>
        <img src="images/blocks/member.gif" alt="Member"> Members: <b>2</b><br>
        <img src="images/blocks/member.gif" alt="Member"> Total: <b>42</b><br>
        <hr>
        <img src="images/blocks/group.gif" alt="Group"> <b><u>Online Now:</u></b><br><img src="modules/MS_Analysis/images/flags/ca.gif" alt="Canada" title="Canada" border="0">&nbsp;<a href="$listenerHref" title="Member Since: Mar 31, 2003"><font class="nick">Listener</font></a>&nbsp;<a href="/modules.php?name=Favorites&amp;user2view=101"><img src="/images/favorites/heart.svg" alt="Public Favorites"></a><br>
        <img src="modules/MS_Analysis/images/flags/blank.gif" alt="" title="">&nbsp;<a href="$PROFILE&u=202"><font class="nick">Member Three</font></a> <img src="images/vip-sst.svg" alt="VIP" title="VIP">&nbsp;<br>
        <hr>
        <img src="images/blocks/group.gif" alt="Group"> <b><u>Legend:</u></b><br>
        <a href="$PROFILE&u=888">Legend Decoy</a>
        </td></tr></table></body></html>
    """.trimIndent()

    /** A members-list page whose rows are numbered from [firstNumber]; [pager] is the markup of the page links. */
    fun membersPage(firstNumber: Int = 1, pager: String = ""): String {
        val second = firstNumber + 1
        val third = firstNumber + 2
        return """
            <html><body>
            <span class="nav">$pager</span>
            <table class="table01" cellspacing="1">
            <th class="th01" colspan="5" height="20">Members List</th>
            <tr>
              <td class="td01">#<br>Status</td><td class="td01">Username<br>Location</td><td class="td01">Poster rank</td>
              <td class="td01">Joined<br>Last Post Date</td><td class="td01">Posts</td>
            </tr>
            <tr>
              <td class="td03">$firstNumber<br><img src="/themes/default/forums/images/lang_english/icon_offline.gif" alt="Listener is offline" title="Listener is offline" /></td>
              <td class="td04"><b><a href="modules.php?name=Forums&file=profile&mode=viewprofile&amp;u=2">Listener</a></b> <a href="modules.php?name=VIP_Subscribe"><img src="images/vip-sst.svg" alt="VIP"></a><br>Springfield, IL &nbsp;<img src="modules/MS_Analysis/images/flags/us.gif" alt="us.gif"></td>
              <td class="td03"><img src="images/ranks/captain.gif" alt="Captain" title="Captain" border="0" /><br />Captain</td>
              <td class="td04">Feb 12, 2002<br>Sun Oct 04, 2026 6:40 pm</td>
              <td class="td03">4942</td>
            </tr>
            <tr>
              <td class="td03">$second<br><img src="/themes/default/forums/images/lang_english/icon_online.gif" alt="ComposerTwo is online" title="ComposerTwo is online" /></td>
              <td class="td04"><b><a href="modules.php?name=Forums&file=profile&mode=viewprofile&amp;u=3">ComposerTwo</a></b> <br>&nbsp; &nbsp;<img src="modules/MS_Analysis/images/flags/blank.gif" alt=""></td>
              <td class="td03"><img src="images/ranks/cadet.gif" alt="Cadet 1" title="Cadet 1" border="0" /><br />Cadet 1</td>
              <td class="td04">Feb 14, 2002<br>None</td>
              <td class="td03">1,204</td>
            </tr>
            <tr>
              <td class="td03">$third<br><img src="/themes/default/forums/images/lang_english/icon_offline.gif" alt="Member Three is offline" title="Member Three is offline" /></td>
              <td class="td04"><b><a href="modules.php?name=Forums&file=profile&mode=viewprofile&amp;u=4">Member Three</a></b> <br>Toronto &nbsp;<img src="modules/MS_Analysis/images/flags/ca.gif" alt="Canada" title="Canada"></td>
              <td class="td03"><img src="images/ranks/ensign.gif" alt="Ensign" title="Ensign" border="0" /><br />Ensign</td>
              <td class="td04">Mar 01, 2002<br>Wed Apr 10, 2002 2:01 am</td>
              <td class="td03">0</td>
            </tr>
            </table>
            </body></html>
        """.trimIndent()
    }

    private fun pagerLink(label: String, start: Int) =
        """<a href="modules.php?name=Members_List&file=index&mode=joined&amp;order=ASC&amp;namepart=&amp;start=$start">$label</a>"""

    /** The page links of the first page: the current page is plain text and later pages are links. */
    fun firstPagePager() =
        "Goto page <b>1</b>, ${pagerLink("2", 50)}, ${pagerLink("3", 100)} ... ${pagerLink("9", 400)}&nbsp;&nbsp;${pagerLink("Next", 50)}"

    /** The page links of a middle page, which also link back to earlier pages. */
    fun middlePagePager() =
        "${pagerLink("Previous", 0)}&nbsp;&nbsp;Goto page ${pagerLink("1", 0)}, <b>2</b>, ${pagerLink("3", 100)}&nbsp;&nbsp;${pagerLink("Next", 100)}"

    /** The page links of the last page: nothing links past the current offset. */
    fun lastPagePager() =
        "${pagerLink("Previous", 350)}&nbsp;&nbsp;Goto page ${pagerLink("1", 0)} ... ${pagerLink("8", 350)}, <b>9</b>"

    fun calendar() = """
        <html><body>
        <table border="0" width="100%" id="table1"><tr><th colspan="2" class="th01">Birthdays &amp; Events Calendar</th></tr>
        <tr><td class="td01">Events &amp; Other Birthdays</td><td class="td01">Member Birthdays</td></tr>
        <tr><td valign="top" class="td02">
        <table cellpadding="4">
        <tr>
        <td align="right" valign="top">January 1</td>
        <td><b>New Year's Day - Theme Day: Opening Titles Day</b><br>
        ComposerOne (53)<br>
        ComposerTwo (61)</td>
        </tr>
        <tr>
        <td align="right" valign="top">March 3</td>
        <td>ComposerThree (70)</td>
        </tr>
        <tr>
        <td align="right" valign="top">February 30</td>
        <td>Nobody (10)</td>
        </tr>
        <tr>
        <td align="right" valign="top">Smarch 3</td>
        <td><b>Not A Month</b></td>
        </tr>
        <tr>
        <td align="right" valign="top">December 31</td>
        <td><b>Theme Day: End Titles Day</b></td>
        </tr>
        </table>
        </td>
        <td valign="top" class="td03">
        <table cellpadding="4">
        <tr>
        <td align="right" valign="top">January 1</td>
        <td><a href="modules.php?name=Your_Account&amp;op=userinfo&amp;username=Listener">Listener</a> (62)<br>
        <a href="modules.php?name=Your_Account&amp;op=userinfo&amp;username=Member%20Three">Member Three</a> (40)</td>
        </tr>
        <tr>
        <td align="right" valign="top">February 14</td>
        <td><a href="modules.php?name=Your_Account&amp;op=userinfo&amp;username=Sweetheart">Sweetheart</a> (30)</td>
        </tr>
        <tr>
        <td align="right" valign="top">March 3</td>
        <td><a href="modules.php?name=Your_Account&amp;op=userinfo&amp;username=Listener">Listener</a> (62)</td>
        </tr>
        </table>
        </td></tr></table>
        <table><tr><td>Donor</td><td>Amount</td><td>${'$'}5.00</td></tr></table>
        </body></html>
    """.trimIndent()
}
