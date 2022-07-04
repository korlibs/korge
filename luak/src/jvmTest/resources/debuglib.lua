
local print,tostring,_G,pcall,ipairs,isnumber = print,tostring,_G,pcall,ipairs,isnumber
local e,f,g,h,s
print( 'has debug', debug~=nil )
if not debug then error( 'no debug' ) end


print( '----- debug.getlocal, debug.setlocal' )
h = function(v,i,n)
	s = 'h-'..v..'-'..i
	local x1,y1 = debug.getlocal(v,i)
	local x2,y2 = debug.setlocal(v,i,n)
	local x3,y3 = debug.getlocal(v,i)
	return s..' -> '..v..'-'..i..' '.. 
		'get='..tostring(x1)..','..tostring(y1)..' '.. 
		'set='..tostring(x2)..','..tostring(y2)..' '.. 
		'get='..tostring(x3)..','..tostring(y3)..' ' 
end
g = function(...)
	local p,q,r=7,8,9
	local t = h(...)
	local b = table.concat({...},',')
	return t..'\tg locals='..p..','..q..','..r..' tbl={'..b..'}'
end
f = function(a,b,c)
	local d,e,f = 4,5,6
	local t = g(a,b,c)
	return t..'\tf locals='..','..a..','..b..','..c..','..d..','..e..','..f
end
for lvl=3,2,-1 do
	for lcl=0,7 do
		print( pcall( f, lvl, lcl, '#' ) ) 
	end
end
for lvl=1,1 do
	for lcl=3,7 do
		print( pcall( f, lvl, lcl, '#' ) ) 
	end
end


print( '----- debug.getupvalue, debug.setupvalue' )
local m,n,o = 101,102,103
f = function(p,q,r)
	local p,q,r = 104,105,106
	local g = function(s,t,u)
		local v,w,x = 107,108,109
		return function()
			return m,n,o,p,q,r,v,w,x
		end
	end
	return g
end
g = f()
h = g()
local callh = function()
	local t = {}
	for i,v in ipairs( { pcall(h) } ) do
		t[i] = tostring(v)
	end
	return table.concat(t,',')
end 
print( 'h', h() )
local funs = { f, g, h }
local names = { 'f', 'g', 'h' }
for i=1,3  do
	local fun,name = funs[i],names[i]	
	for index=0,10 do
		local s1,x1,y1 = pcall( debug.getupvalue, fun, index )
		local s2,x2,y2 = pcall( debug.setupvalue, fun, index, 666000+i*111000+index )
		local s3,x3,y3 = pcall( debug.getupvalue, fun, index )
		print( name..' -> '..i..'-'..index..' '.. 
			'get='..tostring(s1)..','..tostring(x1)..','..tostring(y1)..' '.. 
			'set='..tostring(s2)..','..tostring(x2)..','..tostring(y2)..' '.. 
			'get='..tostring(s3)..','..tostring(x3)..','..tostring(y3)..' '..
			'tbl='..callh() )
	end
end

print( '----- debug.setmetatable, debug.getmetatable' )
local a = {a='bbb'}
local b = {}
local mt = {__index={b='ccc'}}
print( 'a.a='..tostring(a.a)..' a.b='..tostring(a.b)..' b.a='..tostring(b.a)..' b.b='..tostring(b.b)) 
local s1,x1,y1 = pcall( debug.getmetatable, a )
local s2,x2,y2 = pcall( debug.setmetatable, a, mt )
print( 'a.a='..tostring(a.a)..' a.b='..tostring(a.b)..' b.a='..tostring(b.a)..' b.b='..tostring(b.b)) 
local s3,x3,y3 = pcall( debug.getmetatable, a ) print(type(s3), type(x3), type(y3), type(getmetatable(a)))
local s4,x4,y4 = pcall( debug.getmetatable, b ) print(type(s4), type(x4), type(y4), type(getmetatable(b)))
local s5,x5,y5 = pcall( debug.setmetatable, a, nil ) print(type(s5), type(x5), type(y5), type(getmetatable(a)))
print( 'a.a='..tostring(a.a)..' a.b='..tostring(a.b)..' b.a='..tostring(b.a)..' b.b='..tostring(b.b)) 
local s6,x6,y6 = pcall( debug.getmetatable, a ) print(type(s6), type(x6), type(y6), type(getmetatable(a)))
if not s1 then print( 's1 error', x1 ) end
if not s2 then print( 's2 error', x2 ) end
if not s3 then print( 's3 error', x3 ) end
if not s4 then print( 's4 error', x4 ) end
if not s5 then print( 's5 error', x5 ) end
if not s6 then print( 's6 error', x6 ) end
print( 'get='..tostring(s1)..','..tostring(x1==nil)..','..tostring(y1) )
print( 'set='..tostring(s2)..','..tostring(x2==a)..','..tostring(y2) ) 
print( 'get='..tostring(s3)..','..tostring(x3==mt)..','..tostring(y3) ) 
print( 'get='..tostring(s4)..','..tostring(x4==nil)..','..tostring(y4) ) 
print( 'set='..tostring(s5)..','..tostring(x5==a)..','..tostring(y5) ) 
print( 'get='..tostring(s6)..','..tostring(x6==nil)..','..tostring(y6) ) 
print( pcall( debug.getmetatable, 1 ) )
print( pcall( debug.setmetatable, 1, {} ) )
print( pcall( debug.setmetatable, 1, nil ) )

print( '----- debug.getinfo' )
local printfield = function(tbl, field)
	local x = tbl[field]
	if x == nil then return end
	local typ = type(x)
	if typ=='table' then
		x = '{'..table.concat(x,',')..'}'
	elseif typ=='function' then
		x = typ
	end
	print( '    '..field..': '..tostring(x) )
end
local fields = { 'source', 'short_src', 'what', 
	'currentline', 'linedefined', 'lastlinedefined', 
	'nups', 'func', 'activelines' } 
local printinfo = function(...)
	for i,a in ipairs({...}) do
		if type(a) == 'table' then
			for j,field in ipairs(fields) do
				printfield( a, field)
			end
		else
			print( tostring(a) )
		end
	end
end
function test()
	local x = 5
	function f()
		x = x + 1
		return x
	end
	function g()
		x = x - 1
		print( '---' )
		printinfo( 'debug.getinfo(1)', debug.getinfo(1) ) 
		printinfo( 'debug.getinfo(1,"")', debug.getinfo(1, "") ) 
		printinfo( 'debug.getinfo(1,"l")', debug.getinfo(1, "l") ) 
		printinfo( 'debug.getinfo(1,"fL")', debug.getinfo(1, "fL") ) 
		printinfo( 'debug.getinfo(2)', debug.getinfo(2) ) 
		printinfo( 'debug.getinfo(2,"l")', debug.getinfo(2, "l") ) 
		printinfo( 'debug.getinfo(2,"fL")', debug.getinfo(2, "fL") )
		printinfo( 'debug.getinfo(10,"")', pcall( debug.getinfo, 10, "" ) )
		printinfo( 'debug.getinfo(-10,"")', pcall( debug.getinfo, -10, "" ) )
		print( '---' )
		return x
	end
	print(f())
	print(g())
	return f, g
end

local options = "nSlufL"
local e,f,g = pcall( test )
print( 'e,f,g', e, type(f), type(g) )
printinfo( 'debug.getinfo(f)', pcall(debug.getinfo, f) )
printinfo( 'debug.getinfo(f,"'..options..'")', pcall(debug.getinfo, f, options) )
for j=1,6 do
	local opts = options:sub(j,j)
	printinfo( 'debug.getinfo(f,"'..opts..'")', pcall(debug.getinfo, f, opts) )
end 
printinfo( 'debug.getinfo(g)', pcall(debug.getinfo, g) ) 
printinfo( 'debug.getinfo(test)', pcall(debug.getinfo, test) )

print( '----- debug.sethook, debug.gethook' )
f = function(x)
	g = function(y)
		return math.min(x,h)
	end
	local a = g(x)
	return a + a
end
local hook = function(...)
	print( '   ... in hook', ... )
	local info = debug.getinfo(2,"Sl")
	if info then	
		print( ' info[2]='..tostring(info.short_src)..','..tostring(info.currentline) )
	end
end
local tryfunc = function(hook,mask,func,arg)
	local x,f,h,m
	pcall( function()
		debug.sethook(hook,mask)
		x = func(arg)
		f,h,m = debug.gethook()
	end )
	debug.sethook()
	return x,f,h,m
end

local tryhooks = function(mask)
	local s1,a1,b1,c1,d1 = pcall( tryfunc, hook, mask, f, 333 )
	print( 'hook = '..mask..' -> '.. 
		'result='..tostring(s1)..','..tostring(a1)..','..
			type(b1)..','..type(c1)..','..
			tostring(b1==f)..','..tostring(c1==hook)..','..
			tostring(d1)..' ' ) 
end

tryhooks("c")
tryhooks("r")
tryhooks("l")
tryhooks("crl")

print( '----- debug.traceback' )
function test()
	function a(msg)
		print((string.gsub(debug.traceback(msg), "%[Java]", "[C]")))
	end
	local function b(msg)
		pcall(a,msg)
	end
	c = function(i)
		if i <= 0 then b('hi') return end
		return c(i-1)
	end
	d = setmetatable({},{__index=function(t,k) v = c(k) return v end})
	local e = function()
		return d[0]
	end
	local f = {
		g = function()
			e()
		end
	}
	h = function()
		f.g()
	end
	local i = h
	i()
end
pcall(test)


print( '----- debug.upvalueid' )
local x,y = 100,200
function a(b,c)
	local z,w = b,c
	return function()
		x,y,z,w = x+1,y+1,z+1,w+1
		return x,y,z,w
	end
end
a1 = a(300,400)
a2 = a(500,600)
print('debug.getupvalue(a1,1)', debug.getupvalue(a1,1))
print('debug.getupvalue(a1,2)', debug.getupvalue(a1,2))
print('debug.getupvalue(a2,1)', debug.getupvalue(a2,1))
print('debug.getupvalue(a2,2)', debug.getupvalue(a2,2))
print('debug.upvalueid(a1,1) == debug.upvalueid(a1,1)', debug.upvalueid(a1,1) == debug.upvalueid(a1,1))
print('debug.upvalueid(a1,1) == debug.upvalueid(a2,1)', debug.upvalueid(a1,1) == debug.upvalueid(a2,1))
print('debug.upvalueid(a1,1) == debug.upvalueid(a1,2)', debug.upvalueid(a1,1) == debug.upvalueid(a1,2))

print( '----- debug.upvaluejoin' )
print('a1',a1())
print('a2',a2())
print('debug.upvaluejoin(a1,1,a2,2)', debug.upvaluejoin(a1,1,a2,2))
print('debug.upvaluejoin(a1,3,a2,4)', debug.upvaluejoin(a1,3,a2,4))
print('a1',a1())
print('a2',a2())
print('a1',a1())
print('a2',a2())
for i = 1,4 do
	print('debug.getupvalue(a1,'..i..')', debug.getupvalue(a1,i))
	print('debug.getupvalue(a2,'..i..')', debug.getupvalue(a2,i))
	for j = 1,4 do
		print('debug.upvalueid(a1,'..i..') == debug.upvalueid(a1,'..j..')', debug.upvalueid(a1,i) == debug.upvalueid(a1,j))
		print('debug.upvalueid(a1,'..i..') == debug.upvalueid(a2,'..j..')', debug.upvalueid(a1,i) == debug.upvalueid(a2,j))
		print('debug.upvalueid(a2,'..i..') == debug.upvalueid(a1,'..j..')', debug.upvalueid(a2,i) == debug.upvalueid(a1,j))
		print('debug.upvalueid(a2,'..i..') == debug.upvalueid(a2,'..j..')', debug.upvalueid(a2,i) == debug.upvalueid(a2,j))
	end
end
