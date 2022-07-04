package org.luaj.vm2.lib.jse

open class TestClass {
    fun get_PrivateImplClass(): Class<*> = PrivateImpl::class.java

    private class PrivateImpl : TestInterface {
        @JvmField
        var public_field: String

        constructor() {
            this.public_field = "privateImpl-constructor"
        }

        internal constructor(f: String) {
            this.public_field = f
        }

        fun public_method(): String = "privateImpl-$public_field-public_method"
        override fun interface_method(x: String): String = "privateImpl-$public_field-interface_method($x)"
        override fun toString(): String = public_field
    }

    fun create_PrivateImpl(f: String): TestInterface = PrivateImpl(f)

    enum class SomeEnum {
        ValueOne,
        ValueTwo
    }
}

/*
public class TestClass {
   private static class PrivateImpl implements TestInterface {
		public String public_field;
		public PrivateImpl() {
			this.public_field = "privateImpl-constructor";
		}
		PrivateImpl(String f) { 
			this.public_field = f;
		}
		public String public_method() { return "privateImpl-"+public_field+"-public_method"; }
		public String interface_method(String x) { return "privateImpl-"+public_field+"-interface_method("+x+")"; }
		public String toString() { return public_field; }
   }
   public TestInterface create_PrivateImpl(String f) { return new PrivateImpl(f); }
   public Class get_PrivateImplClass() { return PrivateImpl.class; }
   public enum SomeEnum {
       ValueOne,
       ValueTwo,
   }
}
 */
