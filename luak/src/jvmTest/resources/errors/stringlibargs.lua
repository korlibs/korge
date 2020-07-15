package.path = "?.lua;test/lua/errors/?.lua"
require 'args'

-- arg type tests for string library functions

-- string.byte
banner('string.byte')
checkallpass('string.byte',{somestring})
checkallpass('string.byte',{somestring,somenumber})
checkallpass('string.byte',{somestring,somenumber,somenumber})
checkallerrors('string.byte',{somestring,{astring,afunction,atable}},'bad argument')
checkallerrors('string.byte',{notastring,{nil,111,n=2}},'bad argument')

-- string.char
function string_char(...)
	return string.byte( string.char( ... ) )
end
banner('string_char')
checkallpass('string.char',{{60}})
checkallpass('string.char',{{60},{70}})
checkallpass('string.char',{{60},{70},{80}})
checkallpass('string_char',{{0,9,40,127,128,255,'0','9','255','9.2',9.2}})
checkallpass('string_char',{{0,127,255},{0,127,255}})
checkallerrors('string_char',{},'bad argument')
checkallerrors('string_char',{{nil,-1,256,3}},'bad argument')
checkallerrors('string_char',{notanumber,{23,'45',6.7}},'bad argument')
checkallerrors('string_char',{{23,'45',6.7},nonnumber},'bad argument')

-- string.dump
banner('string.dump')
local someupval = 435
local function funcwithupvals() return someupval end
checkallpass('string.dump',{{function() return 123 end}})
checkallpass('string.dump',{{funcwithupvals}})
checkallerrors('string.dump',{notafunction},'bad argument')

-- string.find
banner('string.find')
checkallpass('string.find',{somestring,somestring})
checkallpass('string.find',{somestring,somestring,{nil,-3,3,n=3}})
checkallpass('string.find',{somestring,somestring,somenumber,anylua})
checkallerrors('string.find',{notastring,somestring},'bad argument')
checkallerrors('string.find',{somestring,notastring},'bad argument')
checkallerrors('string.find',{somestring,somestring,nonnumber},'bad argument')

-- string.format
--local numfmts = {'%c','%d','%E','%e','%f','%g','%G','%i','%o','%u','%X','%x'}
local numfmts = {'%c','%d','%i','%o','%u','%X','%x'}
local strfmts = {'%q','%s'}
local badfmts = {'%w'}
banner('string.format')
checkallpass('string.format',{somestring,anylua})
checkallpass('string.format',{numfmts,somenumber})
checkallpass('string.format',{strfmts,somestring})
checkallerrors('string.format',{numfmts,notanumber},'bad argument')
checkallerrors('string.format',{strfmts,notastring},'bad argument')
checkallerrors('string.format',{badfmts,somestring},"invalid option '%w'")

-- string.gmatch
banner('string.gmatch')
checkallpass('string.gmatch',{somestring,somestring})
checkallerrors('string.gmatch',{notastring,somestring},'bad argument')
checkallerrors('string.gmatch',{somestring,notastring},'bad argument')

-- string.gsub
local somerepl = {astring,atable,afunction}
local notarepl = {nil,aboolean,n=2}
banner('string.gsub')
checkallpass('string.gsub',{somestring,somestring,somerepl,{nil,-1,n=2}})
checkallerrors('string.gsub',{nonstring,somestring,somerepl},'bad argument')
checkallerrors('string.gsub',{somestring,nonstring,somerepl},'bad argument')
checkallerrors('string.gsub',{{astring},{astring},notarepl},'bad argument')
checkallerrors('string.gsub',{{astring},{astring},somerepl,nonnumber},'bad argument')

-- string.len
banner('string.len')
checkallpass('string.len',{somestring})
checkallerrors('string.len',{notastring},'bad argument')

-- string.lower
banner('string.lower')
checkallpass('string.lower',{somestring})
checkallerrors('string.lower',{notastring},'bad argument')

-- string.match
banner('string.match')
checkallpass('string.match',{somestring,somestring})
checkallpass('string.match',{somestring,somestring,{nil,-3,3,n=3}})
checkallerrors('string.match',{},'bad argument')
checkallerrors('string.match',{nonstring,somestring},'bad argument')
checkallerrors('string.match',{somestring},'bad argument')
checkallerrors('string.match',{somestring,nonstring},'bad argument')
checkallerrors('string.match',{somestring,somestring,notanumber},'bad argument')

-- string.reverse
banner('string.reverse')
checkallpass('string.reverse',{somestring})
checkallerrors('string.reverse',{notastring},'bad argument')

-- string.rep
banner('string.rep')
checkallpass('string.rep',{somestring,somenumber})
checkallerrors('string.rep',{notastring,somenumber},'bad argument')
checkallerrors('string.rep',{somestring,notanumber},'bad argument')

-- string.sub
banner('string.sub')
checkallpass('string.sub',{somestring,somenumber})
checkallpass('string.sub',{somestring,somenumber,somenumber})
checkallerrors('string.sub',{},'bad argument')
checkallerrors('string.sub',{nonstring,somenumber,somenumber},'bad argument')
checkallerrors('string.sub',{somestring},'bad argument')
checkallerrors('string.sub',{somestring,nonnumber,somenumber},'bad argument')
checkallerrors('string.sub',{somestring,somenumber,nonnumber},'bad argument')

-- string.upper
banner('string.upper')
checkallpass('string.upper',{somestring})
checkallerrors('string.upper',{notastring},'bad argument')

