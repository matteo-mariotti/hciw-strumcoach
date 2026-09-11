package com.example.strumcoach.gamification

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.strumcoach.SessionStats
import java.time.Instant
import java.time.ZoneId

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isUnlocked: (List<SessionStats>) -> Boolean
)

data class LevelInfo(
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    val progress: Float
)

private fun epochDayLocal(epochMs: Long): Long =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

private fun localHour(epochMs: Long): Int =
    Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).hour

object Gamification {

    val allBadges: List<Badge> = listOf(
        Badge(
            id = "first_strum",
            title = "First Strum",
            description = "Complete your first practice session",
            icon = Icons.Default.Star,
            isUnlocked = { sessions -> sessions.any { !it.isReference } }
        ),
        Badge(
            id = "perfectionist",
            title = "Perfectionist",
            description = "Score 95% accuracy or higher in a session",
            icon = Icons.Default.WorkspacePremium,
            isUnlocked = { sessions -> sessions.any { !it.isReference && it.accuracy >= 95 } }
        ),
        Badge(
            id = "consistent",
            title = "Consistent",
            description = "Practice 7 days in a row",
            icon = Icons.Default.LocalFireDepartment,
            isUnlocked = { sessions -> calculateStreak(sessions) >= 7 }
        ),
        Badge(
            id = "dedicated",
            title = "Dedicated",
            description = "Practice 30 days in a row",
            icon = Icons.Default.Whatshot,
            isUnlocked = { sessions -> calculateStreak(sessions) >= 30 }
        ),
        Badge(
            id = "centurion",
            title = "Centurion",
            description = "Complete 100 practice sessions",
            icon = Icons.Default.MilitaryTech,
            isUnlocked = { sessions -> sessions.count { !it.isReference } >= 100 }
        ),
        Badge(
            id = "explorer",
            title = "Explorer",
            description = "Practice 5 different exercises",
            icon = Icons.Default.Explore,
            isUnlocked = { sessions ->
                sessions.filter { !it.isReference && it.exerciseId.isNotEmpty() }
                    .map { it.exerciseId }
                    .distinct()
                    .size >= 5
            }
        ),
        Badge(
            id = "night_owl",
            title = "Night Owl",
            description = "Practice between 10pm and 5am",
            icon = Icons.Default.NightsStay,
            isUnlocked = { sessions ->
                sessions.any { !it.isReference && localHour(it.id).let { h -> h >= 22 || h < 5 } }
            }
        ),
        Badge(
            id = "rising_star",
            title = "Rising Star",
            description = "Reach level 5",
            icon = Icons.Default.EmojiEvents,
            isUnlocked = { sessions -> calculateLevel(calculateXp(sessions)).level >= 5 }
        )
    )

    fun calculateXp(sessions: List<SessionStats>): Int =
        sessions.filter { !it.isReference }.sumOf { 10 + it.accuracy / 2 }

    fun calculateLevel(xp: Int): LevelInfo {
        var level = 1
        var cumulative = 0
        while (true) {
            val xpForThisLevel = 100 * level
            if (cumulative + xpForThisLevel > xp) break
            cumulative += xpForThisLevel
            level++
        }
        val xpForNextLevel = 100 * level
        val xpIntoLevel = xp - cumulative
        return LevelInfo(
            level = level,
            xpIntoLevel = xpIntoLevel,
            xpForNextLevel = xpForNextLevel,
            progress = xpIntoLevel.toFloat() / xpForNextLevel.toFloat()
        )
    }

    fun calculateStreak(sessions: List<SessionStats>): Int {
        val practiceDays = sessions.filter { !it.isReference }
            .map { epochDayLocal(it.id) }
            .toHashSet()
        if (practiceDays.isEmpty()) return 0

        val today = epochDayLocal(System.currentTimeMillis())
        var day = if (practiceDays.contains(today)) today else today - 1
        var streak = 0
        while (practiceDays.contains(day)) {
            streak++
            day--
        }
        return streak
    }

    fun unlockedBadges(sessions: List<SessionStats>): List<Badge> =
        allBadges.filter { it.isUnlocked(sessions) }
}
