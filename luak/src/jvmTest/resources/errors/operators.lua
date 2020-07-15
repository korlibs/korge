package.path = "?.lua;test/lua/errors/?.lua"
require 'args'

-- arg types for language operator

-- ========= unary operators: - # not

-- unary minus -
banner('unary -')
negative = function(a) return - a end
checkallpass('negative',{somenumber})
checkallerrors('negative',{notanumber},'attempt to perform arithmetic')

-- length
banner('#')
lengthop = function(a) return #a end
checkallpass('lengthop',{sometable})
checkallerrors('lengthop',{notatable},'attempt to get length of')

-- length
banner('not')
notop = function(a) return not a end
checkallpass('notop',{somenumber})
checkallpass('notop',{notanumber})

-- function call
banner( '()' )
funcop = function(a) return a() end
checkallpass('funcop',{somefunction})
checkallerrors('funcop',{notafunction},'attempt to call')

-- ========= binary ops: .. + - * / % ^ == ~= <= >= < > [] . and or
banner( '..' )
concatop = function(a,b) return a..b end
checkallpass('concatop',{somestring,somestring})
checkallerrors('concatop',{notastring,somestring},'attempt to concatenate')
checkallerrors('concatop',{somestring,notastring},'attempt to concatenate')

banner( '+' )
plusop = function(a,b) return a+b end
checkallpass('plusop',{somenumber,somenumber})
checkallerrors('plusop',{notanumber,somenumber},'attempt to perform arithmetic')
checkallerrors('plusop',{somenumber,notanumber},'attempt to perform arithmetic')

banner( '-' )
minusop = function(a,b) return a-b end
checkallpass('minusop',{somenumber,somenumber})
checkallerrors('minusop',{notanumber,somenumber},'attempt to perform arithmetic')
checkallerrors('minusop',{somenumber,notanumber},'attempt to perform arithmetic')

banner( '*' )
timesop = function(a,b) return a*b end
checkallpass('timesop',{somenumber,somenumber})
checkallerrors('timesop',{notanumber,somenumber},'attempt to perform arithmetic')
checkallerrors('timesop',{somenumber,notanumber},'attempt to perform arithmetic')

banner( '/' )
divideop = function(a,b) return a/b end
checkallpass('divideop',{somenumber,somenumber})
checkallerrors('divideop',{notanumber,somenumber},'attempt to perform arithmetic')
checkallerrors('divideop',{somenumber,notanumber},'attempt to perform arithmetic')

banner( '%' )
modop = function(a,b) return a%b end
checkallpass('modop',{somenumber,somenumber})
checkallerrors('modop',{notanumber,somenumber},'attempt to perform arithmetic')
checkallerrors('modop',{somenumber,notanumber},'attempt to perform arithmetic')

banner( '^' )
powerop = function(a,b) return a^b end
checkallpass('powerop',{{2,'2.5'},{3,'3.5'}})
checkallerrors('powerop',{notanumber,{3,'3.1'}},'attempt to perform arithmetic')
checkallerrors('powerop',{{2,'2.1'},notanumber},'attempt to perform arithmetic')

banner( '==' )
equalsop = function(a,b) return a==b end
checkallpass('equalsop',{anylua,anylua})

banner( '~=' )
noteqop = function(a,b) return a~=b end
checkallpass('noteqop',{anylua,anylua})

banner( '<=' )
leop = function(a,b) return a<=b end
checkallpass('leop',{{anumber},{anumber}})
checkallpass('leop',{{astring,astrnum},{astring,astrnum}})
checkallerrors('leop',{notanumber,{anumber}},'attempt to compare')
checkallerrors('leop',{{astrnum},{anumber}},'attempt to compare')
checkallerrors('leop',{notastring,{astring,astrnum}},'attempt to compare')
checkallerrors('leop',{{anumber},notanumber},'attempt to compare')
checkallerrors('leop',{{anumber},{astrnum}},'attempt to compare')
checkallerrors('leop',{{astring,astrnum},notastring},'attempt to compare')

banner( '>=' )
geop = function(a,b) return a>=b end
checkallpass('geop',{{anumber},{anumber}})
checkallpass('geop',{{astring,astrnum},{astring,astrnum}})
checkallerrors('geop',{notanumber,{anumber}},'attempt to compare')
checkallerrors('geop',{{astrnum},{anumber}},'attempt to compare')
checkallerrors('geop',{notastring,{astring,astrnum}},'attempt to compare')
checkallerrors('geop',{{anumber},notanumber},'attempt to compare')
checkallerrors('geop',{{anumber},{astrnum}},'attempt to compare')
checkallerrors('geop',{{astring,astrnum},notastring},'attempt to compare')

banner( '<' )
ltop = function(a,b) return a<b end
checkallpass('ltop',{{anumber},{anumber}})
checkallpass('ltop',{{astring,astrnum},{astring,astrnum}})
checkallerrors('ltop',{notanumber,{anumber}},'attempt to compare')
checkallerrors('ltop',{{astrnum},{anumber}},'attempt to compare')
checkallerrors('ltop',{notastring,{astring,astrnum}},'attempt to compare')
checkallerrors('ltop',{{anumber},notanumber},'attempt to compare')
checkallerrors('ltop',{{anumber},{astrnum}},'attempt to compare')
checkallerrors('ltop',{{astring,astrnum},notastring},'attempt to compare')

banner( '>' )
gtop = function(a,b) return a>b end
checkallpass('gtop',{{anumber},{anumber}})
checkallpass('gtop',{{astring,astrnum},{astring,astrnum}})
checkallerrors('gtop',{notanumber,{anumber}},'attempt to compare')
checkallerrors('gtop',{{astrnum},{anumber}},'attempt to compare')
checkallerrors('gtop',{notastring,{astring,astrnum}},'attempt to compare')
checkallerrors('gtop',{{anumber},notanumber},'attempt to compare')
checkallerrors('gtop',{{anumber},{astrnum}},'attempt to compare')
checkallerrors('gtop',{{astring,astrnum},notastring},'attempt to compare')

banner( '[]' )
bracketop = function(a,b) return a[b] end
checkallpass('bracketop',{sometable,notanil})
checkallerrors('bracketop',{notatable,notanil},'attempt to index')
checkallerrors('bracketop',{sometable},'attempt to index')

banner( '.' )
dotop = function(a,b) return a.b end
checkallpass('dotop',{sometable,notanil})
checkallerrors('dotop',{notatable,notanil},'attempt to index')
checkallerrors('dotop',{sometable},'attempt to index')

banner( 'and' )
types = {['table']='table',['function']='function',['thread']='thread'}
clean = function(x) return types[type(x)] or x end
andop = function(a,b) return clean(a and b) end
checkallpass('andop',{anylua,anylua})

banner( 'or' )
orop = function(a,b) return clean(a or b) end
checkallpass('orop',{anylua,anylua})

-- ========= for x in y
banner( 'for x=a,b,c' )
forop = function(a,b,c) for x=a,b,c do end end
checkallpass('forop',{{1,'1.1'},{10,'10.1'},{2,'2.1'}})
checkallerrors('forop',{notanumber,{10,'10.1'},{2,'2.1'}},"'for' initial value must be a number")
checkallerrors('forop',{{1,'1.1'},notanumber,{2,'2.1'}},"'for' limit must be a number")
checkallerrors('forop',{{1,'1.1'},{10,'10.1'},notanumber},"'for' step must be a number")


