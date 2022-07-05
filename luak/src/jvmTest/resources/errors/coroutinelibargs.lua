package.path = "?.lua;test/lua/errors/?.lua"
require 'args'

-- arg type tests for coroutine library functions

-- coroutine.create
banner('coroutine.create')
checkallpass('coroutine.create',{somefunction})
checkallerrors('coroutine.create',{notafunction},'bad argument')

-- coroutine.resume
banner('coroutine.resume')
local co = coroutine.create(function() while true do coroutine.yield() end end)
checkallpass('coroutine.resume',{{co},anylua})
checkallerrors('coroutine.resume',{notathread},'bad argument')

-- coroutine.running
banner('coroutine.running')
checkallpass('coroutine.running',{anylua})

-- coroutine.status
banner('coroutine.status')
checkallpass('coroutine.status',{{co}})
checkallerrors('coroutine.status',{notathread},'bad argument')

-- coroutine.wrap
banner('coroutine.wrap')
checkallpass('coroutine.wrap',{somefunction})
checkallerrors('coroutine.wrap',{notafunction},'bad argument')

-- coroutine.yield
banner('coroutine.yield')
local function f() 
	print( 'yield', coroutine.yield() )
	print( 'yield', coroutine.yield(astring,anumber,aboolean) )
	error('error within coroutine thread')
end
local co = coroutine.create( f )
print( 'status', coroutine.status(co) )
print( coroutine.resume(co,astring,anumber) )
print( 'status', coroutine.status(co) )
print( coroutine.resume(co,astring,anumber) )
print( 'status', coroutine.status(co) )
local s,e = coroutine.resume(co,astring,anumber)
print( s, string.match(e,'error within coroutine thread') or 'bad message: '..tostring(e) )
print( 'status', coroutine.status(co) )

