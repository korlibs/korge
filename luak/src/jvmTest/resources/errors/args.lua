-- utilities to check that args of various types pass or fail 
-- argument type checking
local ok = '-\t'
local fail = 'fail '
local needcheck = 'needcheck '
local badmsg = 'badmsg '

akey      = 'aa'
astring   = 'abc'
astrnum   = '789'
anumber   = 1.25
ainteger  = 345
adouble   = 12.75
aboolean  = true
atable    = {[akey]=456}
afunction = function() end
anil      = nil
athread   = coroutine.create(afunction)

anylua = { nil, astring, anumber, aboolean, atable, afunction, athread }

somestring   = { astring, anumber }
somenumber   = { anumber, astrnum }
someboolean  = { aboolean }
sometable    = { atable }
somefunction = { afunction }
somenil      = { anil }
somekey      = { akey }
notakey      = { astring, anumber, aboolean, atable, afunction }

notastring   = { nil, aboolean, atable, afunction, athread }
notanumber   = { nil, astring, aboolean, atable, afunction, athread }
notaboolean  = { nil, astring, anumber, atable, afunction, athread }
notatable    = { nil, astring, anumber, aboolean, afunction, athread }
notafunction = { nil, astring, anumber, aboolean, atable, athread }
notathread   = { nil, astring, anumber, aboolean, atable, afunction }
notanil      = { astring, anumber, aboolean, atable, afunction, athread }

nonstring   = { aboolean, atable, afunction, athread }
nonnumber   = { astring, aboolean, atable, afunction, athread }
nonboolean  = { astring, anumber, atable, afunction, athread }
nontable    = { astring, anumber, aboolean, afunction, athread }
nonfunction = { astring, anumber, aboolean, atable, athread }
nonthread   = { astring, anumber, aboolean, atable, afunction }
nonkey      = { astring, anumber, aboolean, atable, afunction }

local structtypes = { 
	['table']='<table>',
	['function']='<function>',
	['thread']='<thread>',
	['userdata']='<userdata>',
}

local function bracket(v)
	return "<"..type(v)..">"
end

local function quote(v)
	return "'"..v.."'"
end
	
local function ellipses(v)
	local s = tostring(v)
	return #s <= 8 and s or (string.sub(s,1,8)..'...')
end
	
local pretty = {
	['table']=bracket,
	['function']=bracket,
	['thread']=bracket,
	['userdata']=bracket,
	['string']= quote,
	['number']= ellipses,
}

local function values(list)
	local t = {}
	for i=1,(list.n or #list) do
		local ai = list[i]
		local fi = pretty[type(ai)]
		t[i] = fi and fi(ai) or tostring(ai)
	end
	return table.concat(t,',')
end

local function types(list)
	local t = {}
	for i=1,(list.n or #list) do
		local ai = list[i]
		t[i] = type(ai)
	end
	return table.concat(t,',')
end
	
local function signature(name,arglist)
	return name..'('..values(arglist)..')'
end

local function dup(t)
	local s = {}
	for i=1,(t.n or #t) do
		s[i] = t[i]
	end
	return s
end

local function split(t)
	local s = {}
	local n = (t.n or #t)
	for i=1,n-1 do
		s[i] = t[i]
	end
	return s,t[n]
end

local function expand(argsets, typesets, ...)	
	local arg = table.pack(...)
	local n = typesets and #typesets or 0
	if n <= 0 then
		table.insert(argsets,arg)
		return argsets
	end

	local s,v = split(typesets)
	for i=1,(v.n or #v) do
		expand(argsets, s, v[i], table.unpack(arg,1,arg.n))
	end
	return argsets
end

local function arglists(typesets)
	local argsets = expand({},typesets)
	return ipairs(argsets)	
end

function lookup( name ) 
	return load('return '..name)()
end

function invoke( name, arglist )
	local s,c = pcall(lookup, name)
	if not s then return s,c end
	return pcall(c, table.unpack(arglist,1,arglist.n or #arglist))
end

-- messages, banners
local _print = print
local _tostring = tostring
local _find = string.find
function banner(name)
	_print( '====== '.._tostring(name)..' ======' )
end

local function subbanner(name)
	_print( '--- '.._tostring(name) )
end

local function pack(s,...) 
	return s,{...}
end

-- check that all combinations of arguments pass
function checkallpass( name, typesets, typesonly )
	subbanner('checkallpass')
	for i,v in arglists(typesets) do
		local sig = signature(name,v)
		local s,r = pack( invoke( name, v ) )
		if s then 
			if typesonly then 
				_print( ok, sig, types(r) )
			else
				_print( ok, sig, values(r) )
			end
		else
			_print( fail, sig, values(r) )
		end
	end
end

-- check that all combinations of arguments fail in some way, 
-- ignore error messages
function checkallerrors( name, typesets, template )
	subbanner('checkallerrors')
	template = _tostring(template)
	for i,v in arglists(typesets) do
		local sig = signature(name,v)
		local s,e = invoke( name, v )
		if not s then
			if _find(e, template, 1, true) then
				_print( ok, sig, '...'..template..'...' )
			else
				_print( badmsg, sig, "template='"..template.."' actual='"..e.."'" )
			end
		else
			_print( needcheck, sig, e )
		end
	end
end
