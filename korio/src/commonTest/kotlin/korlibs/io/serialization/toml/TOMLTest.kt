package korlibs.io.serialization.toml

import kotlin.test.Test
import kotlin.test.assertEquals

class TOMLTest {
    @Test
    fun test() {
        assertEquals(
            mapOf(
                "title" to "TOML Example",
                "owner" to mapOf(
                    "name" to "Tom Preston-Werner",
                    "dob" to "1979-05-27T07:32:00-08:00"
                ),
                "database" to mapOf(
                    "enabled" to true,
                    "ports" to listOf(+8000, -8001, 8002),
                    "data" to listOf(listOf("delta", "phi"), listOf(3.14)),
                    "temp_targets" to mapOf("cpu" to 79.5, "case" to 72.0)
                ),
                "servers" to mapOf(
                    "alpha" to mapOf("ip" to "10.0.0.1", "role" to "frontend"),
                    "beta" to mapOf("ip" to "10.0.0.2", "role" to "backend"),
                ),
                "products" to listOf(
                    mapOf("name" to "Hammer", "sku" to 738594937),
                    mapOf(),
                    mapOf("name" to "Nail", "sku" to 284758393, "color" to "gray"),
                ),
                "demo" to mapOf("hello" to mapOf("test" to 10, "demo" to 11, "world" to 12)),
            ),
            TOML.parseToml(
                """
                # This is a TOML document
    
                title = "TOML Example"
    
                [owner]
                name = "Tom Preston-Werner"
                dob = 1979-05-27T07:32:00-08:00
    
                [database]
                enabled = true
                ports = [ +8000, -8001, 8002 ]
                data = [ ["delta", "phi"], [3.14] ]
                temp_targets = { cpu = 79.5, case = 72.0 }
    
                [servers]
    
                [servers.alpha]
                ip = "10.0.0.1"
                role = "frontend"
    
                [servers.beta]
                ip = "10.0.0.2"
                role = "backend"
                
                [[products]]
                name = "Hammer"
                sku = 738594937
                
                [[products]]  # empty table within the array
                
                [[products]]
                name = "Nail"
                sku = 284758393
                
                color = "gray"
                
                [demo]
                hello.test = 10
                hello."demo" = 11
                hello.'world' = 12
            """.trimIndent()
            )
        )
    }
}