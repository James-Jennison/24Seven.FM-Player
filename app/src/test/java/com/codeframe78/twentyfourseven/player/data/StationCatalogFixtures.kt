package com.codeframe78.twentyfourseven.player.data

/** Compact copies of the station pages' structure, with invented identities. */
internal object StationCatalogFixtures {
    fun recentlyAdded() = """
        <html><body><main class="news-listing news-view-recent">
        <article class="news-story"><table class="news-story-meta"><tr><td class="td01"><time datetime="2026-08-27T00:31:32">August 27, 2026</time> &middot; Published by <a href="/modules.php?name=Forums&amp;file=profile&amp;mode=viewprofile&amp;username=Editor">Editor</a></td></tr></table>
        <div class="news-story-body td01"><table class="news-album-table table01"><thead><tr><th class="th01">Album</th></tr></thead><tbody>
        <tr><td class="td01"><a href="/modules.php?name=Album&amp;asin=B000000001"><img class="news-album-cover" src="/images/cover/040/B000000001.jpg" width="40" height="40" alt="">Composer One - First Album</a></td></tr>
        <tr><td class="td01"><a href="/modules.php?name=Album&amp;asin=B000000002"><img class="news-album-cover" src="/images/cover/040/B000000002.jpg" width="40" height="40" alt="">Second, The</a></td></tr>
        <tr><td class="td01"><a href="https://example.com/modules.php?name=Album&amp;asin=B000000003"><img class="news-album-cover" src="https://example.com/B000000003.jpg" alt="">Elsewhere</a></td></tr>
        </tbody></table></div></article>
        <article class="news-story"><table class="news-story-meta"><tr><td class="td01"><time datetime="2026-06-26T09:59:16">June 26, 2026</time> &middot; Published by <a href="/modules.php?name=Forums&amp;file=profile&amp;mode=viewprofile&amp;username=Curator">Curator</a></td></tr></table>
        <div class="news-story-body td01"><table class="news-album-table table01"><thead><tr><th class="th01">Album</th></tr></thead><tbody>
        <tr><td class="td01"><a href="/modules.php?name=Album&amp;asin=B000000004"><img class="news-album-cover" src="/images/cover/040/B000000004.jpg" alt="">Third Album</a></td></tr>
        </tbody></table></div></article>
        </main></body></html>
    """.trimIndent()

    /** An album page with one review; [canWrite] adds the station's write-a-review control. */
    fun albumPage(canWrite: Boolean, author: String = "Reviewer"): String {
        val writeControl = if (canWrite) {
            """<a href="javascript:void(0);" onclick="window.open('https://www.streamingsoundtracks.com/modules.php?name=Album&action=newreview&asin=B000000001', 'reviewWindow', 'width=600,height=550');">Write The First Review</a>"""
        } else {
            ""
        }
        return """
        <html><body><div class="album">
        <table class="table01"><tr><td class="td01">1</td><td class="td01">Opening Theme</td></tr></table>
        <div style="width: 100%; clear: both;">
        <table class="table01 album-review-table" cellspacing="1" cellpadding="2" width="100%">
        <tr><th class="th01">Great Score</th></tr>
        <tr><td class="td01">
            <table class="table01" cellspacing="1" cellpadding="2" width="100%">
                <tr><td class="td01" width="48"><b>By: </b></td><td class="td01" align="left">$author</td></tr>
                <tr><td class="td01"><b>Date: </b></td><td class="td01" align="left">23 Sep 2022</td></tr>
                <tr><td><b>Rating: </b></td><td align="left">
                    <div data-rating-static="1" data-rating-asin="B000000001" class="rating-box" title="4.5 out of 5">
                        <div class="rating-bg"></div><div class="rating-fg" style="width: 90%;"></div>
                    </div>
                </td></tr>
            </table>
        </td></tr>
        <tr><td class="td01" style="padding:20px; line-height: 1.6; text-align: left;">
            Warm themes &amp; <b>strong</b> orchestration.<br><br>Worth a listen.<br><br><div align="center">1 of 2 found this review helpful</div><br><br><div align="center">Please <a href="/modules.php?name=Your_Account">log in</a> to vote on this review</div>        </td></tr>
        </table>
        </div>
        $writeControl
        </div></body></html>
        """.trimIndent()
    }

    fun reviewForm(action: String = "https://streamingsoundtracks.com/modules.php?name=Album&action=submitnewreview&asin=B000000001") = """
        <html><body><form name="reviewform" action="$action" method="post" onSubmit="return checkReviewRating()">
        <input type="text" name="title" maxlength="200">
        <textarea name="content"></textarea>
        <select name="reviewrating"><option value="0">Select</option><option value="5">5.0 - Perfect</option><option value="4.5">4.5 - Excellent</option><option value="4">4.0 - Very Good</option><option value="3.5">3.5 - Good</option><option value="3">3.0 - Fair</option><option value="2.5">2.5 - Below Average</option><option value="2">2.0 - Poor</option><option value="1.5">1.5 - Very Poor</option><option value="1">1.0 - Not Listenable</option></select>
        <input type="submit" value="Submit Review">
        </form></body></html>
    """.trimIndent()

    fun profileEditPage() = """
        <html><body>
        <form action="modules.php?name=SearchGeneral" method="post"><input type="text" name="query"><input type="hidden" name="op" value="search"><input type="submit" value="Search"></form>
        <form action="modules.php?name=Birthday" method="post"><input type="text" name="birthday"><input type="hidden" name="op" value="savebirthday"><input type="submit" value="Save"></form>
        <table><tr><td>
        <form action="modules.php?name=Your_Account" method="post">
        <table>
        <tr><td>Real name:</td><td><input type="text" name="realname" maxlength="60" value="Pat Listener"></td></tr>
        <tr><td>Password:</td><td><input type="password" name="user_password" maxlength="20" value="secret"></td></tr>
        <tr><td>Retype:</td><td><input type="password" name="vpass" maxlength="20"></td></tr>
        <tr><td>Email:</td><td><input type="text" name="user_email" maxlength="255" value="listener@example.org"></td></tr>
        <tr><td>Fake email:</td><td><input type="text" name="femail" maxlength="255"></td></tr>
        <tr><td>Website:</td><td><input type="text" name="user_website" maxlength="255" value="http://www.example.org/listener"></td></tr>
        <tr><td>Location:</td><td><input type="text" name="user_from" maxlength="100" value="Springfield"></td></tr>
        <tr><td>Country:</td><td><select name="user_flag"><option value="blank.gif">SELECT COUNTRY</option><option value="af.gif">Afghanistan</option><option value="us.gif" selected>United States</option></select></td></tr>
        <tr><td>Occupation:</td><td><input type="text" name="user_occ" maxlength="100" value="Engineer"></td></tr>
        <tr><td>Interests:</td><td><input type="text" name="user_interests" maxlength="100" value="Scores &amp; themes"></td></tr>
        <tr><td>Newsletter:</td><td><input type="radio" name="newsletter" value="1" checked> Yes <input type="radio" name="newsletter" value="0"> No</td></tr>
        <tr><td>Hide your online status:</td><td><input type="radio" name="user_allow_viewonline" value="0" checked> Yes <input type="radio" name="user_allow_viewonline" value="1"> No</td></tr>
        <tr><td>Notify:</td><td><input type="radio" name="user_notify" value="1"> Yes <input type="radio" name="user_notify" value="0" checked> No</td></tr>
        <tr><td>Time zone:</td><td><select name="user_timezone"><option value="-5">GMT -5</option><option value="0" selected>GMT</option></select></td></tr>
        <tr><td>Signature:</td><td><textarea name="user_sig">Line one
        Line two</textarea></td></tr>
        <tr><td>Bio:</td><td><textarea name="bio">About me</textarea></td></tr>
        <tr><td colspan="2"><input type="hidden" name="username" value="Listener"><input type="hidden" name="user_id" value="4821"><input type="hidden" name="op" value="saveuser"><input type="submit" value="Save Changes"></td></tr>
        </table>
        </form>
        </td></tr></table>
        <form action="modules.php?name=Your_Account" method="post" enctype="multipart/form-data"><input type="file" name="avatar"><input type="hidden" name="op" value="avatarupload"></form>
        </body></html>
    """.trimIndent()
}
