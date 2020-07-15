local anumber,bnumber   = 111,23.45
local astring,bstring = "abc","def"
local anumstr,bnumstr = tostring(anumber),tostring(bnumber)
local aboolean,bboolean  = false,true
local afunction,bfunction = function() end, function() end
local athread,bthead = coroutine.create(afunction),coroutine.create(bfunction)
local atable,btable    = {},{}
local values = { anumber, aboolean, afunction, athread, atable }
local groups 
local ts = tostring
local tb,count = {},0

tostring = function(o)
	local t = type(o)
	if t~='thread' and t~='function' and t~='table' then return ts(o) end
	if not tb[o] then
		count = count + 1
		tb[o] = t..'.'..count
	end
	return tb[o]
end

local buildop = function(name)
	return function(a,b)
		print( 'mt.__'..name..'()', a, b )
		return '__'..name..'-result'
	end
end
local buildop3 = function(name)
	return function(a,b,c)
		print( 'mt.__'..name..'()', a, b, c )
		return '__'..name..'-result'
	end
end
local buildop1 = function(name)
	return function(a)
		print( 'mt.__'..name..'()', a )
		return '__'..name..'-result'
	end
end

local mt = {
	__call=buildop('call'),
	__add=buildop('add'),
	__sub=buildop('sub'),
	__mul=buildop('mul'),
	__div=buildop('div'),
	__pow=buildop('pow'),
	__mod=buildop('mod'),
	__unm=buildop1('unm'),
	__len=buildop1('len'),
	__lt=buildop('lt'),
	__le=buildop('le'),
	__index=buildop('index'),
	__newindex=buildop3('newindex'),
	__concat=buildop('concat'),
}

-- pcall a function and check for a pattern in the error string
ecall = function(pattern, ...)
	local s,e = pcall(...)
	if not s then e = string.match(e,pattern) or e end
	return s,e
end 

print( '---- __eq same types' )
local eqmt = { 	__eq=buildop('eq'), }
groups = { {nil,nil}, {true,false}, {123,456}, {11,5.5}, {afunction,bfunction}, {athread,bthread}, {astring,bstring}, {anumber,anumstr} }
for i=1,#groups do
	local a,b = groups[i][1], groups[i][2]
	local amt,bmt = debug.getmetatable(a),debug.getmetatable(b)
	print( type(a), type(b), 'before', pcall( function() return a==b end ) )
	print( type(a), type(b), 'before', pcall( function() return a~=b end ) )
	print( debug.setmetatable( a, eqmt ) )
	print( debug.setmetatable( b, eqmt ) )
	print( type(a), type(b), 'after', pcall( function() return a==b end ) )
	print( type(a), type(b), 'after', pcall( function() return a~=b end ) )
	print( debug.setmetatable( a, amt ) )
	print( debug.setmetatable( b, bmt ) )
end

print( '---- __eq, tables - should invoke metatag comparison' )
groups = { {atable,btable} }
for i=1,#groups do
	local a,b = groups[i][1], groups[i][2]
	local amt,bmt = debug.getmetatable(a),debug.getmetatable(b)
	print( type(a), type(b), 'before', pcall( function() return a==b end ) )
	print( type(a), type(b), 'before', pcall( function() return a~=b end ) )
	print( debug.setmetatable( a, eqmt ) )
	print( debug.setmetatable( b, eqmt ) )
	print( type(a), type(b), 'after-a', pcall( function() return a==b end ) )
	print( type(a), type(b), 'after-a', pcall( function() return a~=b end ) )
	print( debug.setmetatable( a, amt ) )
	print( debug.setmetatable( b, bmt ) )
end


print( 'nilmt', debug.getmetatable(nil) )
print( 'boolmt', debug.getmetatable(true) )
print( 'number', debug.getmetatable(1) )
print( 'function', debug.getmetatable(afunction) )
print( 'thread', debug.getmetatable(athread) )

print( '---- __call' )
for i=1,#values do
	local a = values[i]
	local amt = debug.getmetatable(a)
	print( type(a), 'before', ecall( 'attempt to call', function() return a('a','b') end ) )
	print( debug.setmetatable( a, mt ) )
	print( type(a), 'after', pcall( function() return a() end ) )
	print( type(a), 'after', pcall( function() return a('a') end ) )
	print( type(a), 'after', pcall( function() return a('a','b') end ) )
	print( type(a), 'after', pcall( function() return a('a','b','c') end ) )
	print( type(a), 'after', pcall( function() return a('a','b','c','d') end ) )
	print( debug.setmetatable( a, amt ) )
end

print( '---- __add, __sub, __mul, __div, __pow, __mod' )
local groups = { {aboolean, aboolean}, {aboolean, athread}, {aboolean, afunction}, {aboolean, "abc"}, {aboolean, atable} }
for i=1,#groups do
	local a,b = groups[i][1], groups[i][2]
	local amt,bmt = debug.getmetatable(a),debug.getmetatable(b)
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return a+b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return b+a end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return a-b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return b-a end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return a*b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return b*a end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return a^b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return b^a end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return a%b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to perform arithmetic', function() return b%a end ) )
	print( debug.setmetatable( a, mt ) )
	print( type(a), type(b), 'after', pcall( function() return a+b end ) )
	print( type(a), type(b), 'after', pcall( function() return b+a end ) )
	print( type(a), type(b), 'after', pcall( function() return a-b end ) )
	print( type(a), type(b), 'after', pcall( function() return b-a end ) )
	print( type(a), type(b), 'after', pcall( function() return a*b end ) )
	print( type(a), type(b), 'after', pcall( function() return b*a end ) )
	print( type(a), type(b), 'after', pcall( function() return a^b end ) )
	print( type(a), type(b), 'after', pcall( function() return b^a end ) )
	print( type(a), type(b), 'after', pcall( function() return a%b end ) )
	print( type(a), type(b), 'after', pcall( function() return b%a end ) )
	print( debug.setmetatable( a, amt ) )
	print( debug.setmetatable( b, bmt ) )
end

print( '---- __len' )
values = { aboolean, afunction, athread, anumber }
for i=1,#values do
	local a = values[i]
	local amt = debug.getmetatable(a)
	print( type(a), 'before', ecall( 'attempt to get length of ', function() return #a end ) )
	print( debug.setmetatable( a, mt ) )
	print( type(a), 'after', pcall( function() return #a end ) )
	print( debug.setmetatable( a, amt ) )
end
-- 
print( '---- __neg' )
values = { aboolean, afunction, athread, "abcd", atable, anumber }
for i=1,#values do
	local a = values[i]
	local amt = debug.getmetatable(a)
	print( type(v), 'before', ecall( 'attempt to perform arithmetic ', function() return -a end ) )
	print( debug.setmetatable( a, mt ) )
	print( type(v), 'after', pcall( function() return -a end ) )
	print( debug.setmetatable( a, amt ) )
end

print( '---- __lt, __le, same types' )
local bfunction = function() end
local bthread = coroutine.create( bfunction )
local btable = {}
local groups 
groups = { {true, true}, {true, false}, {afunction, bfunction}, {athread, bthread}, {atable, atable}, {atable, btable} }
for i=1,#groups do
	local a,b = groups[i][1], groups[i][2]
	local amt,bmt = debug.getmetatable(a),debug.getmetatable(b)
	print( type(a), type(b), 'before', ecall( 'attempt to compare', function() return a<b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to compare', function() return a<=b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to compare', function() return a>b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to compare', function() return a>=b end ) )
	print( debug.setmetatable( a, mt ) )
	print( debug.setmetatable( b, mt ) )
	print( type(a), type(b), 'after', pcall( function() return a<b end ) )
	print( type(a), type(b), 'after', pcall( function() return a<=b end ) )
	print( type(a), type(b), 'after', pcall( function() return a>b end ) )
	print( type(a), type(b), 'after', pcall( function() return a>=b end ) )
	print( debug.setmetatable( a, amt ) )
	print( debug.setmetatable( b, bmt ) )
end

print( '---- __lt, __le, different types' )
groups = { {aboolean, athread}, }
for i=1,#groups do
	local a,b = groups[i][1], groups[i][2]
	local amt,bmt = debug.getmetatable(a),debug.getmetatable(b)
	print( type(a), type(b), 'before', ecall( 'attempt to compare', function() return a<b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to compare', function() return a<=b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to compare', function() return a>b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to compare', function() return a>=b end ) )
	print( debug.setmetatable( a, mt ) )
	print( debug.setmetatable( b, mt ) )
	print( type(a), type(b), 'after-a', ecall( 'attempt to compare', function() return a<b end ) )
	print( type(a), type(b), 'after-a', ecall( 'attempt to compare', function() return a<=b end ) )
	print( type(a), type(b), 'after-a', ecall( 'attempt to compare', function() return a>b end ) )
	print( type(a), type(b), 'after-a', ecall( 'attempt to compare', function() return a>=b end ) )
	print( debug.setmetatable( a, amt ) )
	print( debug.setmetatable( b, bmt ) )
end

print( '---- __tostring' )
values = { aboolean, afunction, athread, atable, "abc" }
local strmt = { __tostring=function(a)
		return 'mt.__tostring('..type(a)..')'
	end,
}
for i=1,#values do
	local a = values[i]
	local amt = debug.getmetatable(a)
	print( debug.setmetatable( a, strmt ) )
	print( type(a), 'after', pcall( function() return ts(a) end ) )
	print( debug.setmetatable( a, amt ) )
end

print( '---- __index, __newindex' )
values = { aboolean, anumber, afunction, athread }
for i=1,#values do
	local a = values[i]
	local amt = debug.getmetatable(a)
	print( type(a), 'before', ecall( 'attempt to index', function() return a.foo end ) )
	print( type(a), 'before', ecall( 'attempt to index', function() return a[123] end ) )
	print( type(a), 'before', ecall( 'index', function() a.foo = 'bar' end ) )
	print( type(a), 'before', ecall( 'index', function() a[123] = 'bar' end ) )
	print( type(a), 'before', ecall( 'attempt to index', function() return a:foo() end ) )
	print( debug.setmetatable( a, mt ) )
	print( type(a), 'after', pcall( function() return a.foo end ) )
	print( type(a), 'after', pcall( function() return a[123] end ) )
	print( type(a), 'after', pcall( function() a.foo = 'bar' end ) )
	print( type(a), 'after', pcall( function() a[123] = 'bar' end ) )
	print( type(a), 'after', ecall( 'attempt to call', function() return a:foo() end ) )
	print( debug.setmetatable( a, amt ) )
end

print( '---- __concat' )
groups = { {atable, afunction}, {afunction, atable}, {123, nil}, {nil, 123} }
local s,t,u = 'sss',777
local concatresult = setmetatable( { '__concat-result' }, { 
	__tostring=function() 
		return 'concat-string-result' 
	end } )
local concatmt = {
	__concat=function(a,b)
		print( 'mt.__concat('..type(a)..','..type(b)..')', a, b )
		return concatresult
	end
}	
for i=1,#groups do
	local a,b = groups[i][1], groups[i][2]
	local amt,bmt = debug.getmetatable(a),debug.getmetatable(b)
	print( type(a), type(b), 'before', ecall( 'attempt to concatenate ', function() return a..b end ) )
	print( type(a), type(b), 'before', ecall( 'attempt to concatenate ', function() return b..a end ) )
	print( type(a), type(s), type(t), 'before', ecall( 'attempt to concatenate ', function() return a..s..t end ) )
	print( type(s), type(a), type(t), 'before', ecall( 'attempt to concatenate ', function() return s..a..t end ) )
	print( type(s), type(t), type(a), 'before', ecall( 'attempt to concatenate ', function() return s..t..a end ) )
	print( debug.setmetatable( a, concatmt ) )
	print( type(a), type(b), 'after', pcall( function() return a..b end ) )
	print( type(a), type(b), 'after', pcall( function() return b..a end ) )
	print( type(a), type(s), type(t), 'before', pcall( function() return a..s..t end ) )
	print( type(s), type(a), type(t), 'before', ecall( 'attempt to concatenate ', function() return s..a..t end ) )
	print( type(s), type(t), type(a), 'before', ecall( 'attempt to concatenate ', function() return s..t..a end ) )
	print( debug.setmetatable( a, amt ) )
	print( debug.setmetatable( b, bmt ) )
end

print( '---- __metatable' )
values = { aboolean, afunction, athread, atable, "abc" }
local mtmt = { 	__metatable={}, }
for i=1,#values do
	local a = values[i]
	local amt = debug.getmetatable(a)
	print( type(a), 'before', pcall( function() return debug.getmetatable(a), getmetatable(a) end ) )
	print( debug.setmetatable( a, mtmt ) )
	print( type(a), 'after', pcall( function() return debug.getmetatable(a), getmetatable(a) end ) )
	print( debug.setmetatable( a, amt ) )
end
