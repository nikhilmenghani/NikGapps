package com.nikgapps.app.utils.network

import org.junit.Assert.assertEquals
import org.junit.Test

class EliteMembershipRepositoryTest {
    @Test fun sharedFolderRecognizesBothMembersAndKeepsLegacyEntries() {
        val usernames = EliteMembershipRepository.parseUsernames(
            """{"test":["nikhilmenghani","Jayedkhan800"],"legacy":" LegacyUser "}"""
        )
        assertEquals(setOf("nikhilmenghani", "jayedkhan800", "legacyuser"), usernames)
    }

    @Test fun readsGitHubUsernamesRatherThanFolderNames() {
        val usernames = EliteMembershipRepository.parseUsernames(
            """{"custom-folder":"Terminator-J","another":["ExampleUser","SecondUser"],"invalid":42}"""
        )
        assertEquals(setOf("terminator-j", "exampleuser", "seconduser"), usernames)
    }
}
