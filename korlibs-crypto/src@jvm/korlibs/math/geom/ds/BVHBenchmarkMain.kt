/*
package korlibs.math.geom.ds

import korlibs.math.geom.*

object BVHBenchmarkMain {
    @JvmStatic
    fun main(args: Array<String>) {
        for (m in 0 until 1000) {
            val bvh = CSBVH(object : CSBVH.NodeAdaptor<Sphere3D> {
                override fun objectpos(obj: Sphere3D): IVector3 = obj.origin
                override fun radius(obj: Sphere3D): Float = obj.radius
            })
            for (n in 0 until 10000) {
                bvh.addObject(Sphere3D(vec(n.toFloat() * 10f, 0f, 0f), 1f))
            }
            bvh.optimize()

            println(bvh.nodeCount)

            println(
                bvh.traverse(AABB3D(Vector3(-10f, -10f, -10f), Vector3(54f, 10f, 10f)))
                    .flatMap { it.gobjects ?: listOf() })
            //println(bvh.traverse(AABB3D(Vector3(100f, 10f, 10f), Vector3(1500f, 100f, 100f))).map { it.value })
        }
    }
}
*/
