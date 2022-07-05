function printrunning()
	if coroutine.running() == nil then 
		print("running is nil");
	else
		print("running is not nil")
	end
end

function foo (a)
	print("foo", a)
    return coroutine.yield(2*a)
end

co = coroutine.create(function (a,b)
	print("co-body", a, b)
	local r = foo(a+1)
	print("co-body", r)
	local r, s = coroutine.yield(a+b, a-b)
	print("co-body", r, s)

	printrunning()
	print("co.status.inside",coroutine.status(co));
	local co2 = coroutine.create(function()
		print("co.status.inside2",coroutine.status(co));
	end)
	print("co.status.inside",coroutine.status(co));
	coroutine.resume(co2);
		
	return b, "end"
end)

function exercise() 
	printrunning()
	print("co.status",coroutine.status(co));
	print("main", coroutine.resume(co, 1, 10))
	print("co.status",coroutine.status(co));
	print("main", coroutine.resume(co, "r"))
	print("co.status",coroutine.status(co));
	print("main", coroutine.resume(co, "x", "y"))
	print("co.status",coroutine.status(co));
	print("main", coroutine.resume(co, "x", "y"))
	print("co.status",coroutine.status(co));
end

exercise();

co = coroutine.create(function (a,b)
	print("co-body", a, b)
-- TODO: make java and C behave the same for yielding in pcalls
--	local status,r = pcall( foo, a+1 )
foo(a+1)
	print("co-body", status,r)
	local r, s = coroutine.yield(a+b, a-b)
	print("co-body", r, s)
	return b, "end"
end)

exercise();


-- wrap test 
local g = coroutine.wrap(function (a,b)
	print("co-body", a, b)
	local r = foo(a+1)
	print("co-body", r)
	local r, s = coroutine.yield(a+b, a-b)
	print("co-body", r, s)
	return b, "end"
end )

print("g", g(1, 10))
print("g", g("r"))
print("g", g("x", "y"))
local s,e = pcall( g, "x", "y" )
print("g", string.match(e,'cannot resume dead coroutine') or 'badmessage: '..tostring(e))

-- varargs passing
local echo = function(msg,...)
	print( msg, ...)
	return ... 
end
local echocr = function(...)
	local arg = table.pack(...) 
	echo('(echocr) first args', table.unpack(arg,1,arg.n)) 
	local a = arg
	while true do
		a = { echo( '(echoch) yield returns', coroutine.yield( table.unpack(a) ) ) }
	end
end
local c = coroutine.create( echocr )
local step = function(...)
	echo( '(main) resume returns', 
		coroutine.resume(c, echo('(main) sending args', ...)) ) 
end
step(111,222,333)
step()
step(111)
step(111,222,333)

-- test loops in resume calls
b = coroutine.create( function( arg )
	while ( true ) do
		print( '  b-resumed', arg, b == coroutine.running() )
		print( '  b-b', coroutine.status(b) ) 
		print( '  b-c', coroutine.status(c) ) 
		print( '  b-resume-b',coroutine.resume( b, 'b-arg-for-b' ) )
		print( '  b-resume-c',coroutine.resume( c, 'b-arg-for-c' ) )
		arg = coroutine.yield( 'b-rslt' )
	end
end )
c = coroutine.create( function( arg )
	for i=1,3 do
		print( '    c-resumed', arg, c == coroutine.running() )
		print( '    c-b', coroutine.status(b) ) 
		print( '    c-c', coroutine.status(c) ) 
		print( '    c-resume-b',coroutine.resume( b, 'b-arg-for-b' ) )
		print( '    c-resume-c',coroutine.resume( c, 'b-arg-for-c' ) )
		arg = coroutine.yield( 'c-rslt' )
	end
end )
for i=1,3 do
	print( 'main-b', coroutine.status(b) )
	print( 'main-c', coroutine.status(c) )
	print( 'main-resume-b',coroutine.resume( b, 'main-arg-for-b' ) )
	print( 'main-resume-c',coroutine.resume( c, 'main-arg-for-c' ) )
end
