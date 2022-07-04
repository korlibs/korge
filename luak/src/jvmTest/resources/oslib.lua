-- simple os-library tests
--
-- because the nature of the "os" library is to provide os-specific behavior,
-- the compatibility tests must be extremely loose, and can really only 
-- compare things like return value type to be meaningful.
-- 
-- actual os behavior needs to go in an oslib function test
-- 
local pcall = function(...)
	local s,e,f = pcall(...)
	return s,type(e),type(f)
end 
print( 'os', type(os) )
print( 'os.clock()', pcall( os.clock ) )
print( 'os.date()', pcall( os.date ) )
print( 'os.difftime(123000, 21500)', pcall( os.difftime, 123000, 21250 ) )
print( 'os.getenv()', pcall( os.getenv ) )
print( 'os.getenv("bogus.key")', pcall( os.getenv, 'bogus.key' ) )
local s,p = pcall( os.tmpname )
local s,q = pcall( os.tmpname )
print( 'os.tmpname()', s, p )
print( 'os.tmpname()', s, q )
-- permission denied on windows
--print( 'os.remove(p)', pcall( os.remove, p ) )
--print( 'os.rename(p,q)', pcall( os.rename, p, q ) )
local s,f = pcall( io.open, p,"w" )
print( 'io.open', s, f )
print( 'write', pcall( f.write, f, "abcdef 12345" ) )
print( 'close', pcall( f.close, f ) )
print( 'os.rename(p,q)', pcall( os.rename, p, q ) )
print( 'os.remove(q)', pcall( os.remove, q ) )
print( 'os.remove(q)', pcall( os.remove, q ) )
-- setlocale not supported on jse yet
-- print( 'os.setlocale()', pcall( os.setlocale ) )
-- print( 'os.setlocale("jp")', pcall( os.setlocale, "jp" ) )
-- print( 'os.setlocale("us","monetary")', pcall( os.setlocale, "us", "monetary" ) )
-- print( 'os.setlocale(nil,"all")', pcall( os.setlocale, nil, "all" ) )
print( 'os.setlocale("C")', pcall( os.setlocale, "C" ) )
print( 'os.exit', type(os.exit) )

-- os.date() formatting
local t = 1281364496  -- Aug 9, 2010, 2:34:56 PM (Monday)
local function p(s)
  if pcall(os.date, s, t) then
	  print( "os.date('"..s.."', "..t..")", pcall(os.date, s, t))
  end
end
for i= 65, 90 do
  p('%'..string.char(i + 97 - 65))
  p('%'..string.char(i))
end
local tbl = os.date('*t', t)
for i,k in ipairs({'year', 'month', 'day', 'hour', 'min', 'sec', 'wday', 'yday', 'isdst'}) do
	local v = tbl[k]
	print('k', type(k), k, 'v', type(v), v)
end

print('type(os.time())', type(os.time()))
print('os.time({year=1971, month=2, day=25})', os.time({year=1971, month=2, day=25}))
print('os.time({year=1971, month=2, day=25, hour=11, min=22, sec=33})', os.time({year=1971, month=2, day=25, hour=11, min=22, sec=33}))
