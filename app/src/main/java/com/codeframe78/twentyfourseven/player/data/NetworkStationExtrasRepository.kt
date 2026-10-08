package com.codeframe78.twentyfourseven.player.data

import com.codeframe78.twentyfourseven.player.domain.AlbumReviewSendStatus
import com.codeframe78.twentyfourseven.player.domain.AlbumReviewsState
import com.codeframe78.twentyfourseven.player.domain.AlbumReviewsStatus
import com.codeframe78.twentyfourseven.player.domain.CalendarStatus
import com.codeframe78.twentyfourseven.player.domain.EditableProfile
import com.codeframe78.twentyfourseven.player.domain.MAX_ALBUM_REVIEW_BODY_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.MAX_ALBUM_REVIEW_TITLE_CHARACTERS
import com.codeframe78.twentyfourseven.player.domain.MemberFavoritesState
import com.codeframe78.twentyfourseven.player.domain.MemberListSort
import com.codeframe78.twentyfourseven.player.domain.MembersState
import com.codeframe78.twentyfourseven.player.domain.MembersStatus
import com.codeframe78.twentyfourseven.player.domain.MemberFavoritesStatus
import com.codeframe78.twentyfourseven.player.domain.MemberProfileState
import com.codeframe78.twentyfourseven.player.domain.MemberProfileStatus
import com.codeframe78.twentyfourseven.player.domain.PLAYED_HISTORY_BLOCK_HOURS
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryState
import com.codeframe78.twentyfourseven.player.domain.PlayedHistoryStatus
import com.codeframe78.twentyfourseven.player.domain.ProfileEditState
import com.codeframe78.twentyfourseven.player.domain.ProfileEditStatus
import com.codeframe78.twentyfourseven.player.domain.RecentlyAddedStatus
import com.codeframe78.twentyfourseven.player.domain.StationExtrasRepository
import com.codeframe78.twentyfourseven.player.domain.StationExtrasState
import com.codeframe78.twentyfourseven.player.domain.StationId
import com.codeframe78.twentyfourseven.player.domain.StationNewsStatus
import com.codeframe78.twentyfourseven.player.domain.canonicalized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

internal class NetworkStationExtrasRepository(
    private val remote: ExtrasRemoteDataSource,
) : StationExtrasRepository {
    private val states = ConcurrentHashMap<StationId, MutableStateFlow<StationExtrasState>>()
    private val memberLocks = ConcurrentHashMap<StationId, Mutex>()
    private val writeLocks = ConcurrentHashMap<StationId, Mutex>()

    override fun observeExtras(stationId: StationId): Flow<StationExtrasState> =
        state(stationId.canonicalized()).asStateFlow()

    override suspend fun openProfile(stationId: StationId, username: String) {
        val name = username.trim()
        if (name.isEmpty()) return
        val state = state(stationId.canonicalized())
        state.update { it.copy(profile = MemberProfileState(MemberProfileStatus.Loading, name)) }
        val loaded = try {
            val profile = remote.profile(state.value.stationId, name)
            if (profile == null) {
                MemberProfileState(MemberProfileStatus.NotFound, name)
            } else {
                MemberProfileState(MemberProfileStatus.Ready, name, profile)
            }
        } catch (cancellation: CancellationException) {
            state.update { if (it.profile.isLoading(name)) it.copy(profile = MemberProfileState()) else it }
            throw cancellation
        } catch (_: Exception) {
            MemberProfileState(MemberProfileStatus.Error, name)
        }
        // A card the listener already closed, or replaced with another member's, is left alone.
        state.update { if (it.profile.isLoading(name)) it.copy(profile = loaded) else it }
    }

    override suspend fun closeProfile(stationId: StationId) {
        state(stationId.canonicalized()).update { it.copy(profile = MemberProfileState()) }
    }

    override suspend fun openMemberFavorites(stationId: StationId, memberName: String, memberNumber: String) {
        val state = state(stationId.canonicalized())
        state.update { it.copy(memberFavorites = MemberFavoritesState(MemberFavoritesStatus.Loading, memberName)) }
        val loaded = try {
            MemberFavoritesState(
                MemberFavoritesStatus.Ready,
                memberName,
                remote.memberFavorites(state.value.stationId, memberNumber),
            )
        } catch (cancellation: CancellationException) {
            state.update {
                if (it.memberFavorites.isLoading(memberName)) it.copy(memberFavorites = MemberFavoritesState()) else it
            }
            throw cancellation
        } catch (_: FavoritesAuthenticationRequiredException) {
            MemberFavoritesState(MemberFavoritesStatus.SignInRequired, memberName)
        } catch (_: Exception) {
            MemberFavoritesState(MemberFavoritesStatus.Error, memberName)
        }
        state.update { if (it.memberFavorites.isLoading(memberName)) it.copy(memberFavorites = loaded) else it }
    }

    override suspend fun closeMemberFavorites(stationId: StationId) {
        state(stationId.canonicalized()).update { it.copy(memberFavorites = MemberFavoritesState()) }
    }

    override suspend fun loadHistory(stationId: StationId, date: LocalDate, startHour: Int) {
        val block = startHour.coerceIn(0, 24 - PLAYED_HISTORY_BLOCK_HOURS)
            .let { it - it % PLAYED_HISTORY_BLOCK_HOURS }
        val state = state(stationId.canonicalized())
        state.update { it.copy(history = PlayedHistoryState(PlayedHistoryStatus.Loading, date, block)) }
        val loaded = try {
            PlayedHistoryState(
                PlayedHistoryStatus.Ready,
                date,
                block,
                remote.history(state.value.stationId, date, block),
            )
        } catch (cancellation: CancellationException) {
            state.update { if (it.history.isLoading(date, block)) it.copy(history = PlayedHistoryState()) else it }
            throw cancellation
        } catch (_: Exception) {
            PlayedHistoryState(PlayedHistoryStatus.Error, date, block)
        }
        state.update { if (it.history.isLoading(date, block)) it.copy(history = loaded) else it }
    }

    override suspend fun closeHistory(stationId: StationId) {
        state(stationId.canonicalized()).update { it.copy(history = PlayedHistoryState()) }
    }

    override suspend fun refreshNews(stationId: StationId) {
        val state = state(stationId.canonicalized())
        if (state.value.news.status == StationNewsStatus.Loading) return
        state.update { it.copy(news = it.news.copy(status = StationNewsStatus.Loading)) }
        try {
            val stories = remote.news(state.value.stationId)
            state.update { it.copy(news = it.news.copy(status = StationNewsStatus.Ready, stories = stories)) }
        } catch (cancellation: CancellationException) {
            state.update { it.copy(news = it.news.copy(status = StationNewsStatus.Idle)) }
            throw cancellation
        } catch (_: Exception) {
            state.update { it.copy(news = it.news.copy(status = StationNewsStatus.Error)) }
        }
    }

    override suspend fun refreshRecentlyAdded(stationId: StationId) {
        val state = state(stationId.canonicalized())
        if (state.value.recentlyAdded.status == RecentlyAddedStatus.Loading) return
        state.update { it.copy(recentlyAdded = it.recentlyAdded.copy(status = RecentlyAddedStatus.Loading)) }
        try {
            val batches = remote.recentlyAdded(state.value.stationId)
            state.update {
                it.copy(recentlyAdded = it.recentlyAdded.copy(status = RecentlyAddedStatus.Ready, batches = batches))
            }
        } catch (cancellation: CancellationException) {
            state.update { it.copy(recentlyAdded = it.recentlyAdded.copy(status = RecentlyAddedStatus.Idle)) }
            throw cancellation
        } catch (_: Exception) {
            state.update { it.copy(recentlyAdded = it.recentlyAdded.copy(status = RecentlyAddedStatus.Error)) }
        }
    }

    override suspend fun openAlbumReviews(stationId: StationId, albumId: String) {
        val state = state(stationId.canonicalized())
        state.update { it.copy(albumReviews = AlbumReviewsState(albumId, AlbumReviewsStatus.Loading)) }
        val loaded = try {
            val page = remote.albumReviews(state.value.stationId, albumId)
            AlbumReviewsState(albumId, AlbumReviewsStatus.Ready, page.reviews, page.canWrite)
        } catch (cancellation: CancellationException) {
            state.update { if (it.albumReviews.isLoading(albumId)) it.copy(albumReviews = AlbumReviewsState()) else it }
            throw cancellation
        } catch (_: Exception) {
            AlbumReviewsState(albumId, AlbumReviewsStatus.Error)
        }
        state.update { if (it.albumReviews.isLoading(albumId)) it.copy(albumReviews = loaded) else it }
    }

    override suspend fun closeAlbumReviews(stationId: StationId) {
        state(stationId.canonicalized()).update { current ->
            // A review on its way keeps its state, so its result is still reported.
            if (current.albumReviews.sendStatus == AlbumReviewSendStatus.Sending) current else current.copy(albumReviews = AlbumReviewsState())
        }
    }

    override suspend fun submitAlbumReview(stationId: StationId, title: String, body: String, rating: String) {
        val canonical = stationId.canonicalized()
        val state = state(canonical)
        writeLock(canonical).withLock {
            val current = state.value.albumReviews
            val albumId = current.albumId ?: return
            val trimmedTitle = title.trim().take(MAX_ALBUM_REVIEW_TITLE_CHARACTERS)
            val trimmedBody = body.trim().take(MAX_ALBUM_REVIEW_BODY_CHARACTERS)
            if (
                current.status != AlbumReviewsStatus.Ready || !current.canWrite ||
                current.sendStatus == AlbumReviewSendStatus.Sending ||
                trimmedTitle.isEmpty() || trimmedBody.isEmpty()
            ) {
                return
            }
            state.update { it.copy(albumReviews = current.copy(sendStatus = AlbumReviewSendStatus.Sending, sendMessage = null)) }
            val sent = try {
                val page = remote.submitAlbumReview(canonical, albumId, trimmedTitle, trimmedBody, rating)
                val shown = page.reviews.any { it.title.equals(trimmedTitle, ignoreCase = true) }
                current.copy(
                    reviews = page.reviews,
                    canWrite = page.canWrite,
                    sendStatus = if (shown) AlbumReviewSendStatus.Sent else AlbumReviewSendStatus.Unconfirmed,
                    sendMessage = if (shown) "Your review is on the album page." else "The review was sent, but the station does not show it yet.",
                )
            } catch (cancellation: CancellationException) {
                state.update { it.copy(albumReviews = current.copy(sendStatus = AlbumReviewSendStatus.Unconfirmed)) }
                throw cancellation
            } catch (_: FavoritesAuthenticationRequiredException) {
                current.copy(sendStatus = AlbumReviewSendStatus.SignInRequired, sendMessage = "Sign in to the station again to write a review.")
            } catch (_: StationFormUnavailableException) {
                current.copy(canWrite = false, sendStatus = AlbumReviewSendStatus.Rejected, sendMessage = "The station is not taking a review from you for this album.")
            } catch (_: Exception) {
                // The request may have reached the station, so the outcome is unknown rather than failed.
                current.copy(sendStatus = AlbumReviewSendStatus.Error, sendMessage = "The review could not be sent. Check the album page before trying again.")
            }
            state.update { it.copy(albumReviews = sent) }
        }
    }

    override suspend fun openMembers(stationId: StationId) {
        val canonical = stationId.canonicalized()
        val state = state(canonical)
        memberLock(canonical).withLock {
            val current = state.value.members
            state.update { it.copy(members = current.copy(status = MembersStatus.Loading, members = emptyList(), nextStart = null)) }
            val loaded = try {
                val online = remote.onlineNow(canonical)
                val page = remote.members(canonical, current.query, current.sort, 0)
                MembersState(
                    status = MembersStatus.Ready,
                    online = online.members,
                    visitors = online.visitors,
                    totalMembers = online.totalMembers,
                    query = current.query,
                    sort = current.sort,
                    members = page.members,
                    nextStart = page.nextStart,
                )
            } catch (cancellation: CancellationException) {
                state.update { it.copy(members = MembersState()) }
                throw cancellation
            } catch (_: Exception) {
                current.copy(status = MembersStatus.Error)
            }
            state.update { if (it.members.status == MembersStatus.Loading) it.copy(members = loaded) else it }
        }
    }

    override suspend fun searchMembers(stationId: StationId, query: String, sort: MemberListSort) {
        val canonical = stationId.canonicalized()
        val state = state(canonical)
        memberLock(canonical).withLock {
            val current = state.value.members
            if (current.status == MembersStatus.Closed) return
            val trimmed = query.trim().take(MAX_QUERY_CHARACTERS)
            state.update {
                it.copy(members = current.copy(status = MembersStatus.Loading, query = trimmed, sort = sort, members = emptyList(), nextStart = null))
            }
            val loaded = try {
                val page = remote.members(canonical, trimmed, sort, 0)
                current.copy(status = MembersStatus.Ready, query = trimmed, sort = sort, members = page.members, nextStart = page.nextStart)
            } catch (cancellation: CancellationException) {
                state.update { it.copy(members = current.copy(query = trimmed, sort = sort)) }
                throw cancellation
            } catch (_: Exception) {
                current.copy(status = MembersStatus.Error, query = trimmed, sort = sort, members = emptyList(), nextStart = null)
            }
            state.update { if (it.members.status == MembersStatus.Loading) it.copy(members = loaded) else it }
        }
    }

    override suspend fun loadMoreMembers(stationId: StationId) {
        val canonical = stationId.canonicalized()
        val state = state(canonical)
        memberLock(canonical).withLock {
            val current = state.value.members
            val start = current.nextStart ?: return
            if (current.status != MembersStatus.Ready) return
            state.update { it.copy(members = current.copy(status = MembersStatus.Loading)) }
            val loaded = try {
                val page = remote.members(canonical, current.query, current.sort, start)
                val known = current.members.map { it.username.lowercase() }.toHashSet()
                current.copy(
                    status = MembersStatus.Ready,
                    members = (current.members + page.members.filterNot { it.username.lowercase() in known }).take(MAX_MEMBERS),
                    nextStart = page.nextStart?.takeIf { it > start },
                )
            } catch (cancellation: CancellationException) {
                state.update { it.copy(members = current) }
                throw cancellation
            } catch (_: Exception) {
                // The rows already shown stay; only the next page failed.
                current.copy(status = MembersStatus.Ready)
            }
            state.update { if (it.members.status == MembersStatus.Loading) it.copy(members = loaded) else it }
        }
    }

    override suspend fun closeMembers(stationId: StationId) {
        state(stationId.canonicalized()).update { it.copy(members = MembersState()) }
    }

    override suspend fun refreshCalendar(stationId: StationId) {
        val state = state(stationId.canonicalized())
        if (state.value.calendar.status == CalendarStatus.Loading) return
        state.update { it.copy(calendar = it.calendar.copy(status = CalendarStatus.Loading)) }
        try {
            val days = remote.calendar(state.value.stationId)
            state.update { it.copy(calendar = it.calendar.copy(status = CalendarStatus.Ready, days = days)) }
        } catch (cancellation: CancellationException) {
            state.update { it.copy(calendar = it.calendar.copy(status = CalendarStatus.Idle)) }
            throw cancellation
        } catch (_: Exception) {
            state.update { it.copy(calendar = it.calendar.copy(status = CalendarStatus.Error)) }
        }
    }

    override suspend fun openProfileEditor(stationId: StationId) {
        val canonical = stationId.canonicalized()
        val state = state(canonical)
        writeLock(canonical).withLock {
            state.update { it.copy(profileEdit = ProfileEditState(ProfileEditStatus.Loading)) }
            val loaded = try {
                ProfileEditState(ProfileEditStatus.Ready, remote.profileEditForm(canonical))
            } catch (cancellation: CancellationException) {
                state.update { it.copy(profileEdit = ProfileEditState()) }
                throw cancellation
            } catch (_: FavoritesAuthenticationRequiredException) {
                ProfileEditState(ProfileEditStatus.SignInRequired)
            } catch (_: Exception) {
                ProfileEditState(ProfileEditStatus.Error, message = "Your profile could not be loaded right now.")
            }
            state.update { if (it.profileEdit.status == ProfileEditStatus.Loading) it.copy(profileEdit = loaded) else it }
        }
    }

    override suspend fun saveProfile(stationId: StationId, profile: EditableProfile) {
        val canonical = stationId.canonicalized()
        val state = state(canonical)
        writeLock(canonical).withLock {
            val current = state.value.profileEdit
            val form = current.form ?: return
            if (current.status != ProfileEditStatus.Ready && current.status != ProfileEditStatus.Saved) return
            state.update { it.copy(profileEdit = current.copy(status = ProfileEditStatus.Saving, message = null)) }
            val saved = try {
                val kept = remote.saveProfile(canonical, form, profile)
                val expected = profile.copy(flag = form.flags.firstOrNull { it.value == profile.flag }?.value ?: form.profile.flag)
                if (kept.profile.sameEditableValues(expected)) {
                    ProfileEditState(ProfileEditStatus.Saved, kept, "Your profile was saved.")
                } else {
                    ProfileEditState(ProfileEditStatus.Ready, kept, "The station kept the profile shown here; not every change was accepted.")
                }
            } catch (cancellation: CancellationException) {
                state.update { it.copy(profileEdit = current.copy(status = ProfileEditStatus.Ready)) }
                throw cancellation
            } catch (_: FavoritesAuthenticationRequiredException) {
                ProfileEditState(ProfileEditStatus.SignInRequired)
            } catch (_: Exception) {
                // The post may have reached the station; the form is shown again so the member can check it.
                current.copy(status = ProfileEditStatus.Ready, message = "Saving did not complete. Reopen Edit profile to see what the station kept.")
            }
            state.update { it.copy(profileEdit = saved) }
        }
    }

    override suspend fun closeProfileEditor(stationId: StationId) {
        state(stationId.canonicalized()).update { current ->
            if (current.profileEdit.status == ProfileEditStatus.Saving) current else current.copy(profileEdit = ProfileEditState())
        }
    }

    private fun EditableProfile.sameEditableValues(other: EditableProfile): Boolean =
        realName.trim() == other.realName.trim() && location.trim() == other.location.trim() &&
            flag == other.flag && occupation.trim() == other.occupation.trim() &&
            interests.trim() == other.interests.trim() && website.trim() == other.website.trim() &&
            signature.trim() == other.signature.trim() && bio.trim() == other.bio.trim() &&
            newsletter == other.newsletter && hideOnlineStatus == other.hideOnlineStatus

    private fun AlbumReviewsState.isLoading(id: String) = status == AlbumReviewsStatus.Loading && albumId == id

    private fun memberLock(stationId: StationId) = memberLocks.getOrPut(stationId, ::Mutex)

    private fun writeLock(stationId: StationId) = writeLocks.getOrPut(stationId, ::Mutex)

    private fun MemberProfileState.isLoading(name: String) =
        status == MemberProfileStatus.Loading && requestedName == name

    private fun MemberFavoritesState.isLoading(name: String) =
        status == MemberFavoritesStatus.Loading && memberName == name

    private fun PlayedHistoryState.isLoading(date: LocalDate, block: Int) =
        status == PlayedHistoryStatus.Loading && this.date == date && startHour == block

    private fun state(stationId: StationId): MutableStateFlow<StationExtrasState> =
        states.getOrPut(stationId) { MutableStateFlow(StationExtrasState(stationId)) }

    private companion object {
        const val MAX_QUERY_CHARACTERS = 60
        const val MAX_MEMBERS = 2_000
    }
}
