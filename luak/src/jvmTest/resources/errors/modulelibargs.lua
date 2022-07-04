package.path = "?.lua;test/lua/errors/?.lua"
require 'args'

-- arg type tests for module library functions

-- require
banner('require')
checkallpass('require',{{'math','coroutine','package','string','table'}},true)
checkallerrors('require',{{anumber}},'not found')
checkallerrors('require',{{anil,aboolean,afunction,atable}},'bad argument')

-- package.loadlib
banner('package.loadlib')
checkallpass('package.loadlib',{{'foo'},{'bar'}},true)
checkallerrors('package.loadlib',{notastring},'bad argument')

-- package.seeall
banner('package.seeall')
checkallpass('package.seeall',{sometable})
checkallerrors('package.seeall',{notatable},'bad argument')

