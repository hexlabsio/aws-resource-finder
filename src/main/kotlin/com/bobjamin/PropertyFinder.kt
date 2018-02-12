package com.bobjamin

object PropertyFinder{
    fun get(property: String): String? = System.getenv(property) ?: System.getProperty(property)
}