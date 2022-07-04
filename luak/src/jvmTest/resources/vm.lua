
print( '-------- basic vm tests --------' )

print( '-- boolean tests' )
local function booleantests()
	t = true
	f = false
	n = nil
	s = "Hello"
	z = 0
	one = 1
	
	print(t)
	print(f)
	
	print(not t)
	print(not f)
	print(not n)
	print(not z)
	print(not s)
	print(not(not(t)))
	print(not(not(z)))
	print(not(not(n)))
	
	print(t and f)
	print(t or f)
	print(f and t)
	print(f or t)
	
	print(f or one)
	print(f or z)
	print(f or n)
	
	print(t and one)
	print(t and z)
	print(t and n)
end
print( 'booleantests result:', pcall( booleantests ) )


print( '------------- varargs' )
local function varargstest()
	function p(a,...)
		print("a",a)
		print("...",...)
		print("...,a",...,a)
		print("a,...",a,...)
	end
	function q(a,...)
		print("a,arg[1],arg[2],arg[3]",a,arg.n,arg[1],arg[2],arg[3])
	end
	function r(a,...)
		print("a,arg[1],arg[2],arg[3]",a,arg.n,arg[1],arg[2],arg[3])
		print("a",a)
		print("...",...)
		print("...,a",...,a)
		print("a,...",a,...)
	end
	function s(a)
		local arg = { '1', '2', '3' }	
		print("a,arg[1],arg[2],arg[3]",a,arg[1],arg[2],arg[3])
		print("a",a)
	end
	function t(a,...)
		local arg = { '1', '2', '3' }	
		print("a,arg[1],arg[2],arg[3]",a,arg[1],arg[2],arg[3])
		print("a",a)
		print("...",...)
		print("...,a",...,a)
		print("a,...",a,...)
	end
	function u(arg)
		print( 'arg', arg )
	end
	function v(arg,...)
		print( 'arg', arg )
		print("...",...)
		print("arg,...",arg,...)
	end
	arg = { "global-1", "global-2", "global-3" }
	function tryall(f,name)
		print( '---- function '..name..'()' )
		print( '--'..name..'():' )
		print( ' ->', pcall( f ) )
		print( '--'..name..'("q"):' )
		print( ' ->', pcall( f, "q" ) )
		print( '--'..name..'("q","r"):' )
		print( ' ->', pcall( f, "q", "r" ) )
		print( '--'..name..'("q","r","s"):' )
		print( ' ->', pcall( f, "q", "r", "s" ) )
	end
	tryall(p,'p')
	tryall(q,'q')
	tryall(r,'r')
	tryall(s,'s')
	tryall(t,'t')
	tryall(u,'u')
	tryall(v,'v')
end
print( 'varargstest result:', pcall( varargstest ) )

-- The purpose of this test case is to demonstrate that
-- basic metatable operations on non-table types work.
-- i.e. that s.sub(s,...) could be used in place of string.sub(s,...)
print( '---------- metatable tests' )
local function metatabletests()

	-- tostring replacement that assigns ids
	local ts,id,nid,types = tostring,{},0,{table='tbl',thread='thr',userdata='uda',['function']='func'}
	tostring = function(x)
		if not x or not types[type(x)] then return ts(x) end
		if not id[x] then nid=nid+1; id[x]=types[type(x)]..'.'..nid end
		return id[x]
	end
	local results = function(s,e,...)
		if s then return e,... end
		return false,type(e)
	end 
	local pcall = function(...)
		return results( pcall(...) )
	end
		

	local s = "hello"
	print(s:sub(2,4))
	
	local t = {}
	function op(name,...)
		local a,b = pcall( setmetatable, t, ... )
		print( name, t, getmetatable(t), a, b )
	end
	op('set{}  ',{})
	op('set-nil',nil)
	op('set{}  ',{})
	op('set')
	op('set{}  ',{})
	op('set{}  ',{})
	op('set{}{}',{},{})
	op('set-nil',nil)
	op('set{__}',{__metatable={}})
	op('set{}  ',{})
	op('set-nil',nil)
	t = {}
	op('set{}  ',{})
	op('set-nil',nil)
	op('set{__}',{__metatable='abc'})
	op('set{}  ',{})
	op('set-nil',nil)
	
	
	local i = 1234
	local t = setmetatable( {}, {
	   	__mode="v",
	   	__index=function(t,k)
	   	local v = i
	   	i = i + 1
	   	rawset(t,k,v)
	   	return v
	   end,
	} )
	
	local l = { 'a', 'b', 'a', 'b', 'c', 'a', 'b', 'c', 'd' }
	for i,key in ipairs(l) do
		print( 't.'..key, t[key] )
	end
end
print( 'metatabletests result:', pcall( metatabletests ) )

-- test tables with more than 50 elements
print( '------------ huge tables' )
local function hugetables()
	local t = { 1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  		1,1,1,1,1,1,1,1,1,1,
	  }
	print ("#t=",#t,'t[1,50,51,59]', t[1], t[50], t[51], t[59])
	print (table.concat(t,','))
	
	local t2= {	0,3,4,7,9,8,12,15,23,5,
	    	10,13,14,17,19,18,112,115,123,15,
	   	20,33,24,27,29,28,212,215,223,25,
	  	40,43,44,47,49,48,412,415,423,45,
		50,53,54,57,59,58,512,515,523,55,
		60,63,64,67,69,68,612,615,623,65,
		70,73,74,77,79,78,72,715,723,75,
	  }
	
	print ("#t2=",#t2,'t[1,50,51,59]', t[1], t[50], t[51], t[59])
	print (table.concat(t2,','))
	
	local t = { 
		[2000]='a',	[2001]='b', [2002]='c', [2003]='d', [2004]='e', [2005]='f', [2006]='g', [2007]='h', [2008]='i', [2009]='j',
		[3000]='a',	[3001]='b', [3002]='c', [3003]='d', [3004]='e', [3005]='f', [3006]='g', [3007]='h', [3008]='i', [3009]='j',
	 	[4000]='a',	[4001]='b', [4002]='c', [4003]='d', [4004]='e', [4005]='f', [4006]='g', [4007]='h', [4008]='i', [4009]='j',
	 	[5000]='a',	[5001]='b', [5002]='c', [5003]='d', [5004]='e', [5005]='f', [5006]='g', [5007]='h', [5008]='i', [5009]='j',
	 	[6000]='a',	[6001]='b', [6002]='c', [6003]='d', [6004]='e', [6005]='f', [6006]='g', [6007]='h', [6008]='i', [6009]='j',
	 	[7000]='a',	[7001]='b', [7002]='c', [7003]='d', [7004]='e', [7005]='f', [7006]='g', [7007]='h', [7008]='i', [7009]='j',
	 	[8000]='a',	[8001]='b', [8002]='c', [8003]='d', [8004]='e', [8005]='f', [8006]='g', [8007]='h', [8008]='i', [8009]='j',
	}
	
	for i=2000,8000,1000 do
		for j=0,9,1 do
			print( 't['..tostring(i+j)..']', t[i+j] )
		end
	end
end
print( 'hugetables result:', pcall( hugetables ) )

print( '--------- many locals' )
local function manylocals()
	-- test program with more than 50 non-sequential integer elements
	local t0000='a';	local t0001='b'; local t0002='c'; local t0003='d'; local t0004='e'; local t0005='f'; local t0006='g'; local t0007='h'; local t0008='i'; local t0009='j';
	local t1000='a';	local t1001='b'; local t1002='c'; local t1003='d'; local t1004='e'; local t1005='f'; local t1006='g'; local t1007='h'; local t1008='i'; local t1009='j';
	local t2000='a';	local t2001='b'; local t2002='c'; local t2003='d'; local t2004='e'; local t2005='f'; local t2006='g'; local t2007='h'; local t2008='i'; local t2009='j';
	local t3000='a';	local t3001='b'; local t3002='c'; local t3003='d'; local t3004='e'; local t3005='f'; local t3006='g'; local t3007='h'; local t3008='i'; local t3009='j';
	local t4000='a';	local t4001='b'; local t4002='c'; local t4003='d'; local t4004='e'; local t4005='f'; local t4006='g'; local t4007='h'; local t4008='i'; local t4009='j';
	local t5000='a';	local t5001='b'; local t5002='c'; local t5003='d'; local t5004='e'; local t5005='f'; local t5006='g'; local t5007='h'; local t5008='i'; local t5009='j';
	local t6000='a';	local t6001='b'; local t6002='c'; local t6003='d'; local t6004='e'; local t6005='f'; local t6006='g'; local t6007='h'; local t6008='i'; local t6009='j';
	local t7000='a';	local t7001='b'; local t7002='c'; local t7003='d'; local t7004='e'; local t7005='f'; local t7006='g'; local t7007='h'; local t7008='i'; local t7009='j';
	local t8000='a';	local t8001='b'; local t8002='c'; local t8003='d'; local t8004='e'; local t8005='f'; local t8006='g'; local t8007='h'; local t8008='i'; local t8009='j';
	local t9000='a';	local t9001='b'; local t9002='c'; local t9003='d'; local t9004='e'; local t9005='f'; local t9006='g'; local t9007='h'; local t9008='i'; local t9009='j';
	local t10000='a';	local t10001='b'; local t10002='c'; local t10003='d'; local t10004='e'; local t10005='f'; local t10006='g'; local t10007='h'; local t10008='i'; local t10009='j';
	local t11000='a';	local t11001='b'; local t11002='c'; local t11003='d'; local t11004='e'; local t11005='f'; local t11006='g'; local t11007='h'; local t11008='i'; local t11009='j';
	local t12000='a';	local t12001='b'; local t12002='c'; local t12003='d'; local t12004='e'; local t12005='f'; local t12006='g'; local t12007='h'; local t12008='i'; local t12009='j';
	local t13000='a';	local t13001='b'; local t13002='c'; local t13003='d'; local t13004='e'; local t13005='f'; local t13006='g'; local t13007='h'; local t13008='i'; local t13009='j';
	
	-- print the variables
	print(t0000,'a');	print(t0001,'b'); print(t0002,'c'); print(t0003,'d'); print(t0004,'e'); print(t0005,'f'); print(t0006,'g'); print(t0007,'h'); print(t0008,'i'); print(t0009,'j');
	print(t1000,'a');	print(t1001,'b'); print(t1002,'c'); print(t1003,'d'); print(t1004,'e'); print(t1005,'f'); print(t1006,'g'); print(t1007,'h'); print(t1008,'i'); print(t1009,'j');
	print(t2000,'a');	print(t2001,'b'); print(t2002,'c'); print(t2003,'d'); print(t2004,'e'); print(t2005,'f'); print(t2006,'g'); print(t2007,'h'); print(t2008,'i'); print(t2009,'j');
	print(t3000,'a');	print(t3001,'b'); print(t3002,'c'); print(t3003,'d'); print(t3004,'e'); print(t3005,'f'); print(t3006,'g'); print(t3007,'h'); print(t3008,'i'); print(t3009,'j');
	print(t4000,'a');	print(t4001,'b'); print(t4002,'c'); print(t4003,'d'); print(t4004,'e'); print(t4005,'f'); print(t4006,'g'); print(t4007,'h'); print(t4008,'i'); print(t4009,'j');
	print(t5000,'a');	print(t5001,'b'); print(t5002,'c'); print(t5003,'d'); print(t5004,'e'); print(t5005,'f'); print(t5006,'g'); print(t5007,'h'); print(t5008,'i'); print(t5009,'j');
	print(t6000,'a');	print(t6001,'b'); print(t6002,'c'); print(t6003,'d'); print(t6004,'e'); print(t6005,'f'); print(t6006,'g'); print(t6007,'h'); print(t6008,'i'); print(t6009,'j');
	print(t7000,'a');	print(t7001,'b'); print(t7002,'c'); print(t7003,'d'); print(t7004,'e'); print(t7005,'f'); print(t7006,'g'); print(t7007,'h'); print(t7008,'i'); print(t7009,'j');
	print(t8000,'a');	print(t8001,'b'); print(t8002,'c'); print(t8003,'d'); print(t8004,'e'); print(t8005,'f'); print(t8006,'g'); print(t8007,'h'); print(t8008,'i'); print(t8009,'j');
	print(t9000,'a');	print(t9001,'b'); print(t9002,'c'); print(t9003,'d'); print(t9004,'e'); print(t9005,'f'); print(t9006,'g'); print(t9007,'h'); print(t9008,'i'); print(t9009,'j');
	print(t10000,'a');	print(t10001,'b'); print(t10002,'c'); print(t10003,'d'); print(t10004,'e'); print(t10005,'f'); print(t10006,'g'); print(t10007,'h'); print(t10008,'i'); print(t10009,'j');
	print(t11000,'a');	print(t11001,'b'); print(t11002,'c'); print(t11003,'d'); print(t11004,'e'); print(t11005,'f'); print(t11006,'g'); print(t11007,'h'); print(t11008,'i'); print(t11009,'j');
	print(t12000,'a');	print(t12001,'b'); print(t12002,'c'); print(t12003,'d'); print(t12004,'e'); print(t12005,'f'); print(t12006,'g'); print(t12007,'h'); print(t12008,'i'); print(t12009,'j');
	print(t13000,'a');	print(t13001,'b'); print(t13002,'c'); print(t13003,'d'); print(t13004,'e'); print(t13005,'f'); print(t13006,'g'); print(t13007,'h'); print(t13008,'i'); print(t13009,'j');
end
print( 'manylocals result:', pcall( manylocals ) )
