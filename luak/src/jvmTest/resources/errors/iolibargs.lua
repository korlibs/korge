package.path = "?.lua;test/lua/errors/?.lua"
require 'args'

-- arg type tests for io library functions
local f

-- io.close ([file])
banner('io.close')
f = io.open("abc.txt","w")
checkallpass('io.close',{{f}})
checkallerrors('io.close',{notanil},'bad argument')

-- io.input ([file])
banner('io.input')
f = io.open("abc.txt","r")
checkallpass('io.input',{{nil,f,"abc.txt",n=3}})
checkallerrors('io.input',{nonstring},'bad argument')

-- io.lines ([filename])
banner('io.lines')
io.input("abc.txt")
checkallpass('io.lines',{{"abc.txt"}})
checkallerrors('io.lines',{{f}},'bad argument')
checkallerrors('io.lines',{notastring},'bad argument')

-- io.open (filename [, mode])
banner('io.open')
checkallpass('io.open',{{"abc.txt"},{nil,"r","w","a","r+","w+","a+"}})
checkallerrors('io.open',{notastring},'bad argument')
checkallerrors('io.open',{{"abc.txt"},{nonstring}},'bad argument')

-- io.output ([file])
banner('io.output')
checkallpass('io.output',{{nil,f,"abc.txt",n=3}})
checkallerrors('io.output',{nonstring},'bad argument')

-- io.popen (prog [, mode])
banner('io.popen')
--checkallpass('io.popen',{{"hostname"},{nil,"w",n=2}})
checkallerrors('io.popen',{notastring},'bad argument')
checkallerrors('io.popen',{{"hostname"},{nonstring}},'bad argument')

-- io.read (���)
banner('io.read')
checkallpass('io.read',{})
checkallpass('io.read',{{2,"*n","*a","*l"}})
checkallpass('io.read',{{2,"*n","*a","*l"},{2,"*a","*l"}})
checkallerrors('io.read',{{aboolean,afunction,atable,"3"}},'bad argument')

-- io.write (���)
banner('io.write')
checkallpass('io.write',{})
checkallpass('io.write',{somestring})
checkallpass('io.write',{somestring,somestring})
checkallerrors('io.write',{nonstring},'bad argument')
checkallerrors('io.write',{somestring,nonstring},'bad argument')

-- file:write ()
banner('file:write')
file = io.open("seektest.txt","w")
checkallpass('file.write',{{file},somestring})
checkallpass('file.write',{{file},somestring,somestring})
checkallerrors('file.write',{},'bad argument')
checkallerrors('file.write',{{file},nonstring},'bad argument')
checkallerrors('file.write',{{file},somestring,nonstring},'bad argument')
pcall( file.close, file )

-- file:seek ([whence] [, offset])
banner('file:seek')
file = io.open("seektest.txt","r")
checkallpass('file.seek',{{file}})
checkallpass('file.seek',{{file},{"set","cur","end"}})
checkallpass('file.seek',{{file},{"set","cur","end"},{2,"3"}})
checkallerrors('file.seek',{},'bad argument')
checkallerrors('file.seek',{{file},nonstring},'bad argument')
checkallerrors('file.seek',{{file},{"set","cur","end"},nonnumber},'bad argument')

-- file:setvbuf (mode [, size])
banner('file:setvbuf')
checkallpass('file.setvbuf',{{file},{"no","full","line"}})
checkallpass('file.setvbuf',{{file},{"full"},{1024,"512"}})
checkallerrors('file.setvbuf',{},'bad argument')
checkallerrors('file.setvbuf',{{file},notastring},'bad argument')
checkallerrors('file.setvbuf',{{file},{"full"},nonnumber},'bad argument')

