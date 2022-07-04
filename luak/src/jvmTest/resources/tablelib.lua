local func = function(t,...) 
	return (...)
end
local tbl = setmetatable({},{__index=func})
print( tbl[2] )


-- tostring replacement that assigns ids
local ts,id,nid,types = tostring,{},0,{table='tbl',thread='thr',userdata='uda',['function']='func'}
tostring = function(x)
	if not x or not types[type(x)] then return ts(x) end
	if not id[x] then nid=nid+1; id[x]=types[type(x)]..'.'..nid end
	return id[x]
end

local t = { "one", "two", "three", a='aaa', b='bbb', c='ccc' }

table.insert(t,'six');
table.insert(t,1,'seven');
table.insert(t,4,'eight');
table.insert(t,7,'nine');
table.insert(t,10,'ten');  print( #t )

-- concat
print( '-- concat tests' )
function tryconcat(t)
	print( table.concat(t) )
	print( table.concat(t,'--') )
	print( table.concat(t,',',2) )
	print( table.concat(t,',',2,2) )
	print( table.concat(t,',',5,2) )
end
tryconcat( { "one", "two", "three", a='aaa', b='bbb', c='ccc' } )
tryconcat( { "one", "two", "three", "four", "five" } )
function tryconcat(t)
	print( table.concat(t) )
	print( table.concat(t,'--') )
	print( table.concat(t,',',2) )
end
tryconcat( { a='aaa', b='bbb', c='ccc', d='ddd', e='eee' } )
tryconcat( { [501]="one", [502]="two", [503]="three", [504]="four", [505]="five" } )
tryconcat( {} )

-- print the elements of a table in a platform-independent way
function eles(t,f)
	f = f or pairs
	all = {}
	for k,v in f(t) do
		table.insert( all, "["..tostring(k).."]="..tostring(v) )
	end
	table.sort( all )
	return "{"..table.concat(all,',').."}"
end

-- insert, len
print( '-- insert, len tests' )
local t = { "one", "two", "three", a='aaa', b='bbb', c='ccc' }

print( eles(t), #t )
table.insert(t,'six'); print( eles(t), #t )
table.insert(t,1,'seven'); print( eles(t), #t )
table.insert(t,4,'eight'); print( eles(t), #t )
table.insert(t,7,'nine');  print( eles(t), #t )
table.insert(t,10,'ten');  print( eles(t), #t )
print( '#{}', #{} )
print( '#{"a"}', #{"a"} )
print( '#{"a","b"}', #{"a","b"} )
print( '#{"a",nil}', #{"a",nil} )
print( '#{nil,nil}', #{nil,nil} )
print( '#{nil,"b"}', #{nil,"b"}==0 or #{nil,"b"}==2 )
print( '#{"a","b","c"}', #{"a","b","c"} )
print( '#{"a","b",nil}', #{"a","b",nil} )
print( '#{"a",nil,nil}', #{"a",nil,nil} )
print( '#{nil,nil,nil}', #{nil,nil,nil} )
print( '#{nil,nil,"c"}', #{nil,nil,"c"}==0 or #{nil,nil,"c"}==3 )
print( '#{nil,"b","c"}', #{nil,"b","c"}==0 or #{nil,"b","c"}==3 )
print( '#{nil,"b",nil}', #{nil,"b",nil}==0 or #{nil,"b",nil}==2 )
print( '#{"a",nil,"c"}', #{"a",nil,"c"}==1 or #{"a",nil,"c"}==3 )

-- remove
print( '-- remove tests' )
t = { "one", "two", "three", "four", "five", "six", "seven", [10]="ten", a='aaa', b='bbb', c='ccc' }
print( eles(t), #t )
print( 'table.remove(t)', table.remove(t) ); print( eles(t), #t )
print( 'table.remove(t,1)', table.remove(t,1) ); print( eles(t), #t )
print( 'table.remove(t,3)', table.remove(t,3) ); print( eles(t), #t )
print( 'table.remove(t,5)', table.remove(t,5) ); print( eles(t), #t )
print( 'table.remove(t,10)', table.remove(t,10) ); print( eles(t), #t )
print( 'table.remove(t,-1)', table.remove(t,-1) ); print( eles(t), #t )
print( 'table.remove(t,-1)', table.remove(t,-1) ) ; print( eles(t), #t )

-- sort
print( '-- sort tests' )
function sorttest(t,f)
	t = (t)
	print( table.concat(t,'-') )
	if f then
		table.sort(t,f)
	else	
		table.sort(t)
	end
	print( table.concat(t,'-') )
end	
sorttest{ "one", "two", "three" }
sorttest{  "www", "vvv", "uuu", "ttt", "sss", "zzz", "yyy", "xxx" }
sorttest( {  "www", "vvv", "uuu", "ttt", "sss", "zzz", "yyy", "xxx" }, function(a,b) return b<a end)

-- pairs, ipairs
--[[
function testpairs(f, t, name)
	print( name )
	for a,b in f(t) do
		print( ' ', a, b )
	end
end
function testbothpairs(t)
	testpairs( pairs, t, 'pairs( '..eles(t)..' )' )
	testpairs( ipairs, t, 'ipairs( '..eles(t)..' )' )
end
for i,t in ipairs({t0,t1,t2,t3}) do
	testbothpairs(t)
end

t = { 'one', 'two', 'three', 'four', 'five' }
testbothpairs(t)
t[6] = 'six'
testbothpairs(t)
t[4] = nil
testbothpairs(t)
--]]

-- tests of setlist table constructors
-- length is tested elsewhere
print('----- unpack tests -------')
local unpack = table.unpack
print( 'pcall(unpack)', (pcall(unpack)) );
print( 'pcall(unpack,nil)', (pcall(unpack,nil)) );
print( 'pcall(unpack,"abc")', (pcall(unpack,"abc")) );
print( 'pcall(unpack,1)', (pcall(unpack,1)) );
print( 'unpack({"aa"})', unpack({"aa"}) );
print( 'unpack({"aa","bb"})', unpack({"aa","bb"}) );
print( 'unpack({"aa","bb","cc"})', unpack({"aa","bb","cc"}) );
local function a(...) return ... end
print('unpack -',unpack({}))
print('unpack a',unpack({'a'}))
print('unpack .',unpack({nil},1,1))
print('unpack ab',unpack({'a', 'b'}))
print('unpack .b',unpack({nil, 'b'},1,2))
print('unpack a.',unpack({'a', nil},1,2))
print('unpack abc',unpack({'a', 'b', 'c'}))
print('unpack .ab',unpack({nil, 'a', 'b'},1,3))
print('unpack a.b',unpack({'a', nil, 'b'},1,3))
print('unpack ab.',unpack({'a', 'b', nil},1,3))
print('unpack ..b',unpack({nil, nil, 'b'},1,3))
print('unpack a..',unpack({'a', nil, nil},1,3))
print('unpack .b.',unpack({nil, 'b', nil},1,3))
print('unpack ...',unpack({nil, nil, nil},1,3))
print('unpack (-)',unpack({a()}))
print('unpack (a)',unpack({a('a')}))
print('unpack (.)',unpack({a(nil)},1,1))
print('unpack (ab)',unpack({a('a', 'b')}))
print('unpack (.b)',unpack({a(nil, 'b')},1,2))
print('unpack (a.)',unpack({a('a', nil)},1,2))
print('unpack (abc)',unpack({a('a', 'b', 'c')}))
print('unpack (.ab)',unpack({a(nil, 'a', 'b')},1,3))
print('unpack (a.b)',unpack({a('a', nil, 'b')},1,3))
print('unpack (ab.)',unpack({a('a', 'b', nil)},1,3))
print('unpack (..b)',unpack({a(nil, nil, 'b')},1,3))
print('unpack (a..)',unpack({a('a', nil, nil)},1,3))
print('unpack (.b.)',unpack({a(nil, 'b', nil)},1,3))
print('unpack (...)',unpack({a(nil, nil, nil)},1,3))
local t = {"aa","bb","cc","dd","ee","ff"}
print( 'pcall(unpack,t)', pcall(unpack,t) );
print( 'pcall(unpack,t,2)', pcall(unpack,t,2) );
print( 'pcall(unpack,t,2,5)', pcall(unpack,t,2,5) );
print( 'pcall(unpack,t,2,6)', pcall(unpack,t,2,6) );
print( 'pcall(unpack,t,2,7)', pcall(unpack,t,2,7) );
print( 'pcall(unpack,t,1)', pcall(unpack,t,1) );
print( 'pcall(unpack,t,1,5)', pcall(unpack,t,1,5) );
print( 'pcall(unpack,t,1,6)', pcall(unpack,t,1,6) );
print( 'pcall(unpack,t,1,7)', pcall(unpack,t,1,7) );
print( 'pcall(unpack,t,0)', pcall(unpack,t,0) );
print( 'pcall(unpack,t,0,5)', pcall(unpack,t,0,5) );
print( 'pcall(unpack,t,0,6)', pcall(unpack,t,0,6) );
print( 'pcall(unpack,t,0,7)', pcall(unpack,t,0,7) );
print( 'pcall(unpack,t,-1)', pcall(unpack,t,-1) );
print( 'pcall(unpack,t,-1,5)', pcall(unpack,t,-1,5) );
print( 'pcall(unpack,t,-1,6)', pcall(unpack,t,-1,6) );
print( 'pcall(unpack,t,-1,7)', pcall(unpack,t,-1,7) );
print( 'pcall(unpack,t,2,4)', pcall(unpack,t,2,4) );
print( 'pcall(unpack,t,2,5)', pcall(unpack,t,2,5) );
print( 'pcall(unpack,t,2,6)', pcall(unpack,t,2,6) );
print( 'pcall(unpack,t,2,7)', pcall(unpack,t,2,7) );
print( 'pcall(unpack,t,2,8)', pcall(unpack,t,2,8) );
print( 'pcall(unpack,t,2,2)', pcall(unpack,t,2,0) );
print( 'pcall(unpack,t,2,1)', pcall(unpack,t,2,0) );
print( 'pcall(unpack,t,2,0)', pcall(unpack,t,2,0) );
print( 'pcall(unpack,t,2,-1)', pcall(unpack,t,2,-1) );
t[0] = 'zz'
t[-1] = 'yy'
t[-2] = 'xx'
print( 'pcall(unpack,t,0)', pcall(unpack,t,0) );
print( 'pcall(unpack,t,2,0)', pcall(unpack,t,2,0) );
print( 'pcall(unpack,t,2,-1)', pcall(unpack,t,2,-1) );
print( 'pcall(unpack,t,"3")', pcall(unpack,t,"3") );
print( 'pcall(unpack,t,"a")', (pcall(unpack,t,"a")) );
print( 'pcall(unpack,t,function() end)', (pcall(unpack,t,function() end)) );

-- misc tests
print('----- misc table initializer tests -------')
print( # { 'abc', 'def', 'ghi', nil } ) -- should be 3 ! 
print( # { 'abc', 'def', 'ghi', false } ) -- should be 4 ! 
print( # { 'abc', 'def', 'ghi', 0 } ) -- should be 4 ! 

-- basic table operation tests
print('----- basic table operations -------')

local dummyfunc = function(t,...) 
	print( 'metatable call args', type(t), ...)
	return 'dummy' 
end
local makeloud = function(t)
	return setmetatable(t,{
		__index=function(t,k)
			print( '__index', type(t), k )
			return rawset(t,k)
		end,
		__newindex=function(t,k,v)
			print( '__newindex', type(t), k, v )
			rawset(t,k,v)
		end})
end
local tests = {
	{'basic table', {}},
	{'function metatable on __index', setmetatable({},{__index=dummyfunc})},
 	{'function metatable on __newindex', setmetatable({},{__newindex=dummyfunc})},
	{'plain metatable on __index', setmetatable({},makeloud({}))},
	{'plain metatable on __newindex', setmetatable({},makeloud({}))},
}
local function shoulderr(s,e)
	return s,type(e)
end
for i,test in ipairs(tests) do
	local testname = test[1]
	local testtable = test[2]
	print( '------ basic table tests on '..testname..' '..type(testtable) )
	print( 't[1]=2',     pcall( function() testtable[1]=2 end ) )
	print( 't[1]',       pcall( function() return testtable[1] end ) )
	print( 't[1]=nil',   pcall( function() testtable[1]=nil end ) )
	print( 't[1]',       pcall( function() return testtable[1] end ) )
	print( 't["a"]="b"', pcall( function() testtable["a"]="b" end ) )
	print( 't["a"],t.a', pcall( function() return testtable["a"],testtable.a end ) )
	print( 't.a="c"',    pcall( function() testtable.a="c" end ) )
	print( 't["a"],t.a', pcall( function() return testtable["a"],testtable.a end ) )
	print( 't.a=nil',    pcall( function() testtable.a=nil end ) )
	print( 't["a"],t.a', pcall( function() return testtable["a"],testtable.a end ) )
	print( 't[nil]="d"', shoulderr( pcall( function() testtable[nil]="d" end ) ) )
	print( 't[nil]',     pcall( function() return testtable[nil] end ) )
	print( 't[nil]=nil', shoulderr( pcall( function() testtable[nil]=nil end ) ) )
	print( 't[nil]',     pcall( function() return testtable[nil] end ) )
end

print( '-- sort tests' )
local function tryall(cmp)
	local function try(t)
		print( table.concat(t,'-') )
		if pcall( table.sort, t, cmp ) then
			print( table.concat(t,'-') )
		else
			print( 'sort failed' )
		end
	end
	try{ 2, 4, 6, 8, 1, 3, 5, 7 }
	try{ 333, 222, 111 }
	try{ "www", "xxx", "yyy", "aaa", "bbb", "ccc" }
	try{ 21, 23, "25", 27, 22, "24", 26, 28 }
end
local function comparator(a,b)
	return tonumber(a)<tonumber(b) 
end
print ( 'default (lexical) comparator' )
tryall()
print ( 'custom (numerical) comparator' )
tryall(comparator)
