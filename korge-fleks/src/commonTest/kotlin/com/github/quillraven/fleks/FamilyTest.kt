package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.compareEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class FamilyTest {

    private val testEntityService = EntityService(64, ComponentService(mapOf()))

    private val emptyTestFamily = Family(entityService = testEntityService)
    private fun createCmpBitmask(cmpIdx: Int): BitArray? {
        return if (cmpIdx > 0) {
            BitArray().apply { set(cmpIdx) }
        } else {
            null
        }
    }


    @Test
    fun testContains() {
        val testCases = listOf(
            arrayOf(
                "empty family contains entity without components",
                BitArray(), // entity component mask
                0, 0, 0,    // family allOf, noneOf, anyOf indices
                true        // expected
            ),
            arrayOf(
                "empty family contains entity with any components",
                BitArray().apply { set(1) }, // entity component mask
                0, 0, 0,                     // family allOf, noneOf, anyOf indices
                true                         // expected
            ),
            arrayOf(
                "family with allOf does not contain entity without components",
                BitArray(), // entity component mask
                1, 0, 0,    // family allOf, noneOf, anyOf indices
                false       // expected
            ),
            arrayOf(
                "family with allOf contains entity with specific component",
                BitArray().apply { set(1) }, // entity component mask
                1, 0, 0,                     // family allOf, noneOf, anyOf indices
                true                         // expected
            ),
            arrayOf(
                "family with noneOf contains entity without components",
                BitArray(), // entity component mask
                0, 1, 0,    // family allOf, noneOf, anyOf indices
                true        // expected
            ),
            arrayOf(
                "family with noneOf does not contain entity with specific component",
                BitArray().apply { set(1) }, // entity component mask
                0, 1, 0,                     // family allOf, noneOf, anyOf indices
                false                        // expected
            ),
            arrayOf(
                "family with anyOf does not contain entity without components",
                BitArray(), // entity component mask
                0, 0, 1,    // family allOf, noneOf, anyOf indices
                false       // expected
            ),
            arrayOf(
                "family with anyOf contains entity with specific component",
                BitArray().apply { set(1) }, // entity component mask
                0, 0, 1,                     // family allOf, noneOf, anyOf indices
                true                         // expected
            ),
        )

        testCases.forEach {
            val eCmpMask = it[1] as BitArray
            val fAllOf = createCmpBitmask(it[2] as Int)
            val fNoneOf = createCmpBitmask(it[3] as Int)
            val fAnyOf = createCmpBitmask(it[4] as Int)
            val family = Family(fAllOf, fNoneOf, fAnyOf, testEntityService)
            val expected = it[5] as Boolean

            assertEquals(expected, eCmpMask in family)
        }
    }

    @Test
    fun updateActiveEntities() {
        val family = emptyTestFamily

        family.onEntityCfgChanged(Entity(0), BitArray())
        family.updateActiveEntities()

        assertFalse { family.isDirty }
        assertEquals(1, family.entitiesBag.size)
        assertEquals(0, family.entitiesBag[0])
    }

    @Test
    fun callActionForEachEntity() {
        val family = emptyTestFamily
        family.onEntityCfgChanged(Entity(0), BitArray())
        family.updateActiveEntities()
        var processedEntity = -1
        var numExecutions = 0

        family.forEach {
            numExecutions++
            processedEntity = it.id
        }

        assertEquals(0, processedEntity)
        assertEquals(1, numExecutions)
    }

    @Test
    fun sortEntities() {
        val family = emptyTestFamily
        family.onEntityCfgChanged(Entity(0), BitArray())
        family.onEntityCfgChanged(Entity(2), BitArray())
        family.onEntityCfgChanged(Entity(1), BitArray())
        family.updateActiveEntities()

        // sort descending by entity id
        family.sort(compareEntity { e1, e2 -> e2.id.compareTo(e1.id) })

        assertEquals(2, family.entitiesBag[0])
        assertEquals(1, family.entitiesBag[1])
        assertEquals(0, family.entitiesBag[2])
    }

    @Test
    fun testOnEntityCfgChange() {
        val testCases = listOf(
            // first = add entity to family before calling onChange
            // second = make entity part of family
            Pair(false, false),
            Pair(false, true),
            Pair(true, false),
            Pair(true, true),
        )

        testCases.forEach {
            val family = Family(BitArray().apply { set(1) }, null, null, testEntityService)
            val addEntityBeforeCall = it.first
            val addEntityToFamily = it.second
            val entity = Entity(1)
            if (addEntityBeforeCall) {
                family.onEntityCfgChanged(entity, BitArray().apply { set(1) })
                family.updateActiveEntities()
            }

            if (addEntityToFamily) {
                family.onEntityCfgChanged(entity, BitArray().apply { set(1) })

                assertEquals(!addEntityBeforeCall, family.isDirty)
            } else {
                family.onEntityCfgChanged(entity, BitArray())

                assertEquals(addEntityBeforeCall, family.isDirty)
            }
        }
    }

    @Test
    fun testNestedIteration() {
        // delayRemoval and cleanup should only get called once for the first iteration
        val f1 = Family(entityService = testEntityService)
        val f2 = Family(entityService = testEntityService)
        testEntityService.addEntityListener(f1)
        testEntityService.addEntityListener(f2)
        val e1 = testEntityService.create { }
        testEntityService.create { }
        var remove = true

        var numOuterIterations = 0
        var numInnerIterations = 0
        f1.forEach {
            assertTrue { this.entityService.delayRemoval }

            f2.forEach {
                assertTrue { this.entityService.delayRemoval }
                if (remove) {
                    remove = false
                    entityService.remove(e1)
                }
                ++numInnerIterations
            }
            ++numOuterIterations

            // check that inner iteration is not clearing the delayRemoval flag
            assertTrue { this.entityService.delayRemoval }
            // check that inner iteration is not cleaning up the delayed removals
            assertEquals(0, this.entityService.removedEntities.length())
        }

        assertFalse { f1.entityService.delayRemoval }
        assertEquals(1, f1.entityService.removedEntities.length())
        assertEquals(2, numOuterIterations)
        assertEquals(4, numInnerIterations)
    }

    @Test
    fun testFamilyListener() {
        // FamilyListener creation internally retrieves a correct family
        // of the related world for the registration part in the world's constructor
        // --> fake it in this test via DummyComponent
        class DummyComponent

        World.CURRENT_WORLD = world {
            components {
                add(::DummyComponent)
            }
        }

        val requiredCmps = BitArray().apply { set(1) }
        val e = Entity(0)
        val listener = object : FamilyListener(
            allOfComponents = arrayOf(DummyComponent::class)
        ) {
            var numAdd = 0
            var numRemove = 0

            override fun onEntityAdded(entity: Entity) {
                ++numAdd
            }

            override fun onEntityRemoved(entity: Entity) {
                ++numRemove
            }
        }
        val family = Family(allOf = requiredCmps, entityService = testEntityService)

        family.addFamilyListener(listener)
        assertTrue { listener in family }

        family.onEntityCfgChanged(e, requiredCmps)
        assertEquals(1, listener.numAdd)
        assertEquals(0, listener.numRemove)

        family.onEntityCfgChanged(e, BitArray())
        assertEquals(1, listener.numAdd)
        assertEquals(1, listener.numRemove)

        family.removeFamilyListener(listener)
        assertFalse { listener in family }
    }
}
