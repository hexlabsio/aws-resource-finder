package com.bobjamin

import junit.framework.TestCase.assertEquals
import org.junit.Test

class NodeTest{
    @Test
    fun `should find multiple group sets`(){
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
        assertEquals(2, relatedGroups.size)
        assertEquals(listOf(
                "A" to "D",
                "A" to "C",
                "A" to "G",
                "E" to "D",
                "C" to "E",
                "G" to "H"
        ), relatedGroups[0])
        assertEquals(listOf(
                "B" to "F"
        ), relatedGroups[1])
    }

    @Test
    fun `should find no groups in an empty list`(){
        assertEquals(0, emptyList<Pair<Any,Any>>().relatedGroups().size)
    }

    @Test
    fun `should find 1 group in a singleton list`(){
        val relationships = listOf("A" to "B")
        val relatedGroups = relationships.relatedGroups()
        assertEquals(1, relatedGroups.size)
        assertEquals(relationships[0], relatedGroups[0][0])
    }

    @Test
    fun `should find group when self references exist`(){
        val relationships = listOf("A" to "A")
        val relatedGroups = relationships.relatedGroups()
        assertEquals(1, relatedGroups.size)
        assertEquals(relationships[0], relatedGroups[0][0])
    }

    @Test
    fun `should find 1 group in a list of linked pairs`(){
        val relationships = listOf("A" to "B", "B" to "C")
        val relatedGroups = relationships.relatedGroups()
        assertEquals(1, relatedGroups.size)
        assertEquals(relationships[0], relatedGroups[0][0])
        assertEquals(relationships[1], relatedGroups[0][1])
    }

    @Test
    fun `should find 1 group in a list of linked pairs even with recursive`(){
        val relationships = listOf("A" to "B", "B" to "C", "C" to "A")
        val relatedGroups = relationships.relatedGroups()
        assertEquals(1, relatedGroups.size)
        assertEquals("A" to "B", relatedGroups[0][0])
        assertEquals( "C" to "A", relatedGroups[0][1])
        assertEquals("B" to "C", relatedGroups[0][2])
    }
}