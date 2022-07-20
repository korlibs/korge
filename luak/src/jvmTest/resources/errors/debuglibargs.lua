package.path = "?.lua;test/lua/errors/?.lua"
require 'args'

local alevel = {25,'25'}
local afuncorlevel = {afunction,25,'25'}
local notafuncorlevel = {nil,astring,aboolean,athread}
local notafuncorthread = {nil,astring,aboolean}

-- debug.debug()
banner('debug.debug - no tests')

-- debug.gethook ([thread])
banner('debug.gethook')
checkallpass('debug.gethook',{})

-- debug.getinfo ([thread,] f [, what])
banner('debug.getinfo')
local awhat = {"","n","flnStu"}
local notawhat = {"qzQZ"}
checkallpass('debug.getinfo',{afuncorlevel})
checkallpass('debug.getinfo',{somethread,afuncorlevel})
checkallpass('debug.getinfo',{afuncorlevel,awhat})
checkallpass('debug.getinfo',{somethread,afuncorlevel,awhat})
checkallerrors('debug.getinfo',{},'function or level')
checkallerrors('debug.getinfo',{notafuncorlevel},'function or level')
checkallerrors('debug.getinfo',{somefunction,nonstring}, 'string expected')
checkallerrors('debug.getinfo',{notafuncorthread,somefunction}, 'string expected')
checkallerrors('debug.getinfo',{nonthread,somefunction,{astring}}, 'string expected')
checkallerrors('debug.getinfo',{somethread,somefunction,notawhat}, 'invalid option')

-- debug.getlocal ([thread,] f, local)
banner('debug.getlocal')
local p,q = 'p','q';
f = function(x,y)
	print('f: x,y,a,b,p,q', x, y, a, b, p, q)
	local a,b = x,y
	local t = coroutine.running()
	checkallpass('debug.getlocal',{{f,1},{1,'2'}})
	checkallpass('debug.getlocal',{{t},{f},{1}})
	checkallerrors('debug.getlocal',{},'number expected')
	checkallerrors('debug.getlocal',{afuncorlevel,notanumber},'number expected')
	checkallerrors('debug.getlocal',{notafuncorlevel,somenumber}, 'number expected')
	checkallerrors('debug.getlocal',{{t},afuncorlevel}, 'got no value')
	checkallerrors('debug.getlocal',{nonthread,{f},{1,'2'}}, 'number expected')
	checkallerrors('debug.getlocal',{{t},{100},{1}}, 'level out of range')
end
f(1,2)

-- debug.getmetatable (value)
banner('debug.getmetatable')
checkallpass('debug.getmetatable',{anylua})
checkallerrors('debug.getmetatable',{},'value expected')

-- debug.getregistry ()
banner('debug.getregistry')
checkallpass('debug.getregistry',{})
checkallpass('debug.getregistry',{anylua})

-- debug.getupvalue (f, up)
banner('debug.getupvalue')
checkallpass('debug.getupvalue',{{f},{1,'2'}})
checkallerrors('debug.getupvalue',{},'number expected')
checkallerrors('debug.getupvalue',{notafunction,{1,'2'}}, 'function expected')
checkallerrors('debug.getupvalue',{{f},notanumber}, 'number expected')

-- debug.getuservalue (u)
checkallpass('debug.getuservalue',{})
checkallpass('debug.getuservalue',{anylua})

-- debug.sethook ([thread,] hook, mask [, count])
local ahookstr = {"cr","l"}
checkallpass('debug.sethook',{})
checkallpass('debug.sethook',{somenil,ahookstr})
checkallpass('debug.sethook',{somefunction,ahookstr})
checkallpass('debug.sethook',{{nil,athread,n=2},somefunction,ahookstr})
checkallerrors('debug.sethook',{{astring,afunction,aboolean}},'string expected')
checkallerrors('debug.sethook',{{astring,afunction,aboolean},{nil,afunction,n=2},ahookstr},'string expected')

-- debug.setlocal ([thread,] level, local, value)
banner('debug.setlocal')
local p,q = 'p','q';
f = function(x,y)
	print('f: x,y,a,b,p,q', x, y, a, b, p, q)
	local a,b = x,y
	local t = coroutine.running()
	checkallpass('debug.setlocal',{{1},{1},{nil,'foo',n=2}})
	print('f: x,y,a,b,p,q', x, y, a, b, p, q)
	checkallpass('debug.setlocal',{{t},{1},{2},{nil,'bar',n=2}})
	print('f: x,y,a,b,p,q', x, y, a, b, p, q)
	checkallerrors('debug.setlocal',{},'number expected')
	checkallerrors('debug.setlocal',{{1}},'value expected')
	checkallerrors('debug.setlocal',{{1},{1}}, 'value expected')
	checkallerrors('debug.setlocal',{{t},{1},{2}}, 'value expected')
	checkallerrors('debug.setlocal',{{atable,astring},{1}}, 'number expected')
	checkallerrors('debug.setlocal',{{1},nonnumber}, 'value expected')
	checkallerrors('debug.setlocal',{{atable,astring},{1},{1},{nil,'foo',n=2}}, 'number expected')
	checkallerrors('debug.setlocal',{{10},{1},{'foo'}}, 'level out of range')
	return p,q
end
f(1,2)

-- debug.setmetatable (value, table)
banner('debug.setmetatable')
checkallpass('debug.setmetatable',{anylua,{atable,nil,n=2}})
checkallerrors('debug.setmetatable',{},'nil or table')
checkallerrors('debug.setmetatable',{anylua},'nil or table')

-- debug.setupvalue (f, up, value)
banner('debug.setupvalue')
checkallpass('debug.setupvalue',{{f},{2,'3'},{nil,aboolean,astring,n=3}})
print('p,q', p, q)
checkallerrors('debug.setupvalue',{},'value expected')
checkallerrors('debug.setupvalue',{{f}},'value expected')
checkallerrors('debug.setupvalue',{{f},{2}},'value expected')
checkallerrors('debug.setupvalue',{notafunction,{2}}, 'value expected')
checkallerrors('debug.setupvalue',{{f},notanumber}, 'value expected')

-- debug.setuservalue (udata, value)
banner('debug.setuservalue')
checkallerrors('debug.setuservalue',{},'userdata expected')
checkallerrors('debug.setuservalue',{anylua},'userdata expected')
checkallerrors('debug.setuservalue',{anylua,somestring},'userdata expected')

-- debug.traceback ([thread,] [message [, level]])
banner('debug.traceback')
local t = coroutine.running()
checkallpass('debug.traceback',{})
checkallpass('debug.traceback',{{astring}})
checkallpass('debug.traceback',{{astring},{anumber}})
checkallpass('debug.traceback',{{t}})
checkallpass('debug.traceback',{{t},{astring}})
checkallpass('debug.traceback',{{t},{astring},{anumber}})
checkallpass('debug.traceback',{{afunction,aboolean,atable}})
checkallpass('debug.traceback',{{afunction,aboolean,atable},notanumber})

-- debug.upvalueid (f, n)
banner('debug.upvalueid')
checkallpass('debug.upvalueid',{{f},{1,'2'}})
checkallerrors('debug.upvalueid',{},'number expected')
checkallerrors('debug.upvalueid',{notafunction,{1,'2'}}, 'function expected')
checkallerrors('debug.upvalueid',{{f},notanumber}, 'number expected')

-- debug.upvaluejoin (f1, n1, f2, n2)
banner('debug.upvaluejoin')
checkallpass('debug.upvaluejoin',{{f},{1,'2'},{f},{1,'2'}})
checkallerrors('debug.upvaluejoin',{},'number expected')
checkallerrors('debug.upvaluejoin',{notafunction,{1,'2'}}, 'function expected')
checkallerrors('debug.upvaluejoin',{{f},notanumber}, 'number expected')
checkallerrors('debug.upvaluejoin',{{f},{1},notafunction,{1,'2'}}, 'function expected')
checkallerrors('debug.upvaluejoin',{{f},{1},{f},notanumber}, 'number expected')
