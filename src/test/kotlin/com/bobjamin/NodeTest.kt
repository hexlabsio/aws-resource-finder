package com.bobjamin

import org.junit.Test

class NodeTest{
    @Test
    fun `groups`(){
        val relationships = listOf(
                "A" to "D",
                "A" to "C",
                "C" to "E",
                "B" to "F",
                "E" to "D",
                "A" to "G",
                "G" to "H"
        )
        val relatedGroups = relationships.relatedGroups()
        val a = ""
    }
}