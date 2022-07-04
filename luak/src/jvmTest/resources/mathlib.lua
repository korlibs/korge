local platform = ...
--print( 'platform', platform )

local aliases = {
	['0']='<zero>',
	['-0']='<zero>',
	['nan']='<nan>',
	['inf']='<pos-inf>',
	['-inf']='<neg-inf>',
	['1.#INF']='<pos-inf>',
	['-1.#INF']='<neg-inf>',
	['1.#IND']='<nan>',
	['-1.#IND']='<nan>',
}
	
local UNOPVALUES = { -2.5, -2, 0, 2, 2.5, "'-2.5'", "'-2'", "'0'", "'2'", "'2.5'" }

local NUMBERPAIRS = {
	{ 2, 0 }, { -2.5, 0 }, { 2, 1 }, 
	{ 5, 2 }, {-5, 2 }, {16, 2}, {-16, -2}, 
	{ .5, 0}, {.5, 1}, {.5, 2}, {.5, -1}, {.5, 2},
	{2.25, 0}, {2.25, 2}, {-2, 0},
	{ 3, 3 }, 
}

local STRINGPAIRS = {
	{ "'2'", "'0'" }, { "'2.5'","'3'" }, { "'-2'", "'1.5'" }, { "'-2.5'", "'-1.5'" },
	{ "'3.0'", "'3.0'" }, { 2.75, 2.75 }, { "'2.75'", "'2.75'" }, 
}

local MIXEDPAIRS = {
	{ 3, "'3'" }, { "'3'", 3 }, { 2.75, "'2.75'" }, { "'2.75'", 2.75 }, 
	{ -3, "'-4'" }, { "'-3'", 4 }, { -3, "'4'" }, { "'-3'", -4 },  
	{ -4.75, "'2.75'" }, { "'-2.75'", 1.75 }, { 4.75, "'-2.75'" }, { "'2.75'", -1.75 }, 
}

local BINOPVALUES = {}

local RELATIONALOPVALUES = {}

local function addall( t, s ) 
	for i,v in ipairs(s) do 
		t[#t+1] = v 
	end
end
addall( BINOPVALUES, NUMBERPAIRS )
addall( BINOPVALUES, STRINGPAIRS )
addall( BINOPVALUES, MIXEDPAIRS )
addall( RELATIONALOPVALUES, NUMBERPAIRS )
addall( RELATIONALOPVALUES, STRINGPAIRS )
	 
local VARARGSVALUES = {
	{ 4, }, { -4.5 }, { "'5.5'" }, { "'-5'" }, 
	{ 4, "'8'" }, { -4.5, "'-8'" }, { "'5.5'", 2.2 }, { "'-5'", -2.2 }, 
	{ 111,222,333 }, { -222,-333,-111 }, { 444,-111,-222 },
}
	 
local CONSTANTS = { 
	"huge", "pi", 
}
local UNARYOPS = {
	"-", "not ",
}
local BINARYOPS = {
	"+", "-", "*", "^", "/", "%", 
}
local RELATIONALS = {
	"==", "~=", ">", "<", ">=", "<=",  
}
local ONEARG_JME = {
	"abs", "ceil", "cos", "deg", 
	"exp", "floor", "frexp", "modf",
	"rad", "sin", "sqrt", "tan", 
}
local ONEARG_JSE = {
	"acos", "asin", "atan", "cosh",  
	"log", "sinh", "tanh",
}
local TWOARGS_JME = {
	"fmod", "ldexp", "pow", 
}
local TWOARGS_JSE = {
	"atan2",
}
local VARARGSFUNCS = {
	"max", "min", 
}

local ts = tostring
tostring = function(x)
	local s = ts(x)
	if type(x)~='number' then return s end
	if aliases[s] then return aliases[s] end
	if #s < 7 then return s end
	local a,b = string.match(s,'([%-0-9%.]*)([eE]?[%-0-9]*)')
	return a and (string.sub(a,1,6)..(b or '')) or s
end

local function eval( expr, script )
	script = script or ('return '..expr)
	local s,a,b = load( script, 'expr' )
	if s then print( expr, pcall( s ) ) 
	else print( expr, 'load:', a ) end
end

-- misc tests
print( '---------- miscellaneous tests ----------' )
eval( 'math.sin( 0.0 )' ) 
eval( 'math.cos( math.pi )' ) 
eval( 'math.sqrt( 9.0 )' ) 
eval( 'math.modf( 5.25 )') 
eval( 'math.frexp(0.00625)' ) 
eval( '-5 ^ 2' ) 
eval( '-5 / 2' ) 
eval( '-5 % 2' ) 

-- constants 
print( '---------- constants ----------' )
for i,v in ipairs(CONSTANTS) do
	eval( 'math.'..v )
end

-- unary operators 
for i,v in ipairs(UNARYOPS) do
	print( '---------- unary operator '..v..' ----------' )
	for j,a in ipairs(UNOPVALUES) do
		eval( v..a, 'return '..v..a )
	end
end

-- binary operators
for i,v in ipairs(BINARYOPS) do
	print( '---------- binary operator '..v..' ----------' )
	for j,xy in ipairs(BINOPVALUES) do
		eval( xy[1]..v..xy[2],
			'local x,y='..xy[1]..','..xy[2]..'; return x'..v..'y' ) 
	end
end

-- relational operators
for i,v in ipairs(RELATIONALS) do
	print( '---------- binary operator '..v..' ----------' )
	for j,xy in ipairs(RELATIONALOPVALUES) do
		eval( xy[1]..v..xy[2],
			'local x,y='..xy[1]..','..xy[2]..'; return x'..v..'y' ) 
	end
end

-- one-argument math functions
for i,v in ipairs(ONEARG_JME) do
	print( '---------- math.'..v..' ----------' )
	for j,x in ipairs(UNOPVALUES) do
		eval( 'math.'..v..'('..x..')' )
	end
end
if platform ~= 'JME' then 
	for i,v in ipairs(ONEARG_JSE) do
		print( '---------- math.'..v..' (jse only) ----------' )
		for j,x in ipairs(UNOPVALUES) do
			eval( 'math.'..v..'('..x..')' )
		end
	end
end

-- two-argument math functions
for i,v in ipairs(TWOARGS_JME) do
	print( '---------- math.'..v..' ----------' )
	for j,x in ipairs(BINOPVALUES) do
		eval( 'math.'..v..'('..x[1]..','..x[2]..')' )
	end
end
if platform ~= 'JME' then 
	for i,v in ipairs(TWOARGS_JSE) do
		print( '---------- math.'..v..' (jse only) ----------' )
		for j,x in ipairs(BINOPVALUES) do
			eval( 'math.'..v..'('..x[1]..','..x[2]..')' )
		end
	end
end

-- var-arg math functions
for i,v in ipairs(VARARGSFUNCS) do
	print( '---------- math.'..v..' ----------' )
	for j,x in ipairs(VARARGSVALUES) do
		eval( 'math.'..v..'('..table.concat(x,',')..')' )
	end
end

-- random tests
print("----------- Random number tests")
local function testrandom(string,lo,hi)
	local c,e = load('return '..string)
	for i=1,5 do 
		local s,e = pcall(c) 
		if s then
			print( string, s and type(e) or e, (e>=lo) and (e<=hi) )
		else 
			print( string, 'error', e )
		end
	end
end
testrandom('math.random()',0,1)
testrandom('math.random(5,10)',5,10)
testrandom('math.random(30)',0,30)
testrandom('math.random(-4,-2)',-4,-2)
local t = {} 
print( math.randomseed(20) )
for i=1,20 do
	t[i] = math.random()
end
print( '-- comparing new numbers')
for i=1,20 do
	print( t[i] == math.random(), t[i] == t[0] )
end
print( '-- resetting seed')

print( math.randomseed(20) )
for i=1,20 do
	print( t[i] == math.random() )
end

-- tests involving -0, which is folded into 0 for luaj, but not for plain lua
print("----------- Tests involving -0 and NaN")
print('0 == -0', 0 == -0)
t = {[0] = 10, 20, 30, 40, 50}
print('t[-0] == t[0]',t[-0] == t[0])
local x = -1
local mz, z = 0/x, 0 -- minus zero, zero
print('mz, z', mz, z)
print('mz == z', mz == z)
local a = {[mz] = 1}
print('a[z] == 1 and a[mz] == 1', a[z] == 1 and a[mz] == 1)
-- string with same binary representation as 0.0 (may create problems
-- for constant manipulation in the pre-compiler)
local a1, a2, a3, a4, a5 = 0, 0, "\0\0\0\0\0\0\0\0", 0, "\0\0\0\0\0\0\0\0"
assert(a1 == a2 and a2 == a4 and a1 ~= a3)
assert(a3 == a5)

--]]
