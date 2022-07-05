
print( '-------- simple upvalues tests --------' )
local function simpleupvalues()
	function test()
		local x = 5
		function f()
			x = x + 1
			return x
		end
		function g()
			x = x - 1
			return x
		end
		print(f())
		print(g())
		return f, g
	end
	
	f1, g1 = test()
	print("f1()=", f1())
	print("g1()=", g1())
	
	f2, g2 = test()
	print("f2()=", f2())
	print("g2()=", g2())
	
	print("g1()=", g1())
	print("f1()=", f1())
end
print( 'simplevalues result:', pcall( simpleupvalues ) )


-- The point of this test is that when an upvalue is created, it may
-- need to be inserted in the middle of the list, rather than always
-- appended at the end. Otherwise, it may not be found when it is
-- needed by another closure.
print( '----------- upvalued in middle ------------' )
local function middleupvaluestest()
	local function test()
	   local x = 3
	   local y = 5
	   local z = 7
	   
	   local function f()
	      print("y=", y)
	   end
	   
	   local function g()
	      print("z=", z)
	   end
	   
	   local function h()
	      print("x=", x)
	   end
	   
	   local function setter(x1, y1, z1)
	      x = x1
	      y = y1
	      z = z1
	   end
	   
	   return f, g, h, setter
	end
	
	local f, g, h, setter = test()
	
	h()
	f()
	g()
	
	setter("x", "y", "z")
	
	h()
	f()
	g()
end
print( pcall( middleupvaluestest ) )


print( '--------- nested upvalues ----------' )
local function nestedupvaluestest()
	local f	
	do
		local x = 10
		function g()
			print(x, f())
		end
	end
	
	function f()
		return 20
	end
	
	g()
end
print( 'nestedupvaluestest result:', pcall( nestedupvaluestest ) )

