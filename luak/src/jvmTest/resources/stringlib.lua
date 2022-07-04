print( string.find("1234567890", ".", 0, true) )
print( string.find( 'alo alx 123 b\0o b\0o', '(..*) %1' ) )
print( string.find( 'aloALO', '%l*' ) )
print( string.find( ' \n isto � assim', '%S%S*' ) )

print( string.find( "", "" ) )
print( string.find( "ababaabbaba", "abb" ) )
print( string.find( "ababaabbaba", "abb", 7 ) )

print( string.match( "aabaa", "a*" ) )
print( string.match( "aabaa", "a*", 3 ) )
print( string.match( "aabaa", "a*b" ) )
print( string.match( "aabaa", "a*b", 3 ) )

print( string.match( "abbaaababaabaaabaa", "b(a*)b" ) )

print( string.match( "abbaaababaabaaabaa", "b(a*)()b" ) )
print( string.match( "abbaaababaabaaabaa", "b(a*)()b", 3 ) )
print( string.match( "abbaaababaabaaabaa", "b(a*)()b", 8 ) )
print( string.match( "abbaaababaabaaabaa", "b(a*)()b", 12 ) )

print( string.byte("hi", -3) )

print( string.gsub("ABC", "@(%x+)", function(s) return "|abcd" end) )
print( string.gsub("@123", "@(%x+)", function(s) return "|abcd" end) )
print( string.gsub("ABC@123", "@(%x+)", function(s) return "|abcd" end) )
print( string.gsub("ABC@123@def", "@(%x+)", function(s) return "|abcd" end) )
print( string.gsub("ABC@123@qrs@def@tuv", "@(%x+)", function(s) return "|abcd" end) )
print( string.gsub("ABC@123@qrs@def@tuv", "@(%x+)", function(s) return "@ab" end) )

print( tostring(1234567890123) )
print( tostring(1234567890124) )
print( tostring(1234567890125) )

function f1(s, p)
  print(p)
  p = string.gsub(p, "%%([0-9])", function (s) return "%" .. (s+1) end)
  print(p)
  p = string.gsub(p, "^(^?)", "%1()", 1)
  print(p)
  p = string.gsub(p, "($?)$", "()%1", 1)
  print(p)
  local t = {string.match(s, p)}
  return string.sub(s, t[1], t[#t] - 1)
end

print( pcall( f1, 'alo alx 123 b\0o b\0o', '(..*) %1' ) )

local function badpat()
	print( string.gsub( "alo", "(.)", "%2" ) )
end

print( ( pcall( badpat ) ) )

for k, v in string.gmatch("w=200&h=150", "(%w+)=(%w+)") do
    print(k, v)
end

-- string.sub
function t(str)
	local i = { 0, 1, 2, 8, -1 }
	for ki,vi in ipairs(i) do
		local s,v = pcall( string.sub, str, vi )
		print( 'string.sub("'..str..'",'..tostring(vi)..')='..tostring(s)..',"'..tostring(v)..'"' )
		local j = { 0, 1, 2, 4, 8, -1 }
		for kj,vj in ipairs(j) do
			local s,v = pcall( string.sub, str, vi, vj )
			print( 'string.sub("'..str..'",'..tostring(vi)..','..tostring(vj)..')='..tostring(s)..',"'..tostring(v)..'"' )
		end
	end
end
t( 'abcdefghijklmn' )
t( 'abcdefg' )
t( 'abcd' )
t( 'abc' )
t( 'ab' )
t( 'a' )
t( '' )

print(string.len("Hello, world"))
print(#"Hello, world")
print(string.len("\0\0\0"))
print(#"\0\0\0")
print(string.len("\0\1\2\3"))
print(#"\0\1\2\3")
local s = "My JaCk-O-lAnTeRn CaSe TeXt"
print(s, string.len(s), #s)


-- string.format
print(string.format("(%.0d) (%.0d) (%.0d)", 0, -5, 9))
print(string.format("(%.1d) (%.1d) (%.1d)", 0, -5, 9))
print(string.format("(%.2d) (%.2d) (%.2d)", 0, -5, 9))

print(string.format("(%+.0d) (%+.0d) (%+.0d)", 0, -5, 9))
print(string.format("(%+.1d) (%+.1d) (%+.1d)", 0, -5, 9))
print(string.format("(%+.2d) (%+.2d) (%+.2d)", 0, -5, 9))

print(string.format("(%+3d) (% 3d) (%+ 3d)", 55, 55, 55))

print(string.format("(%-1d) (%-1d) (%-1d)", 1, 12, -12))
print(string.format("(%-2d) (%-2d) (%-2d)", 1, 12, -12))
print(string.format("(%-3d) (%-3d) (%-3d)", 1, 12, -12))


print(string.format("(%8x) (%8d) (%8o)", 255, 255, 255))
print(string.format("(%08x) (%08d) (%08o)", 255, 255, 255))

print(string.format("simple%ssimple", " simple "))

local testformat = function(message,fmt,...)
	local s,e = pcall( string.format, fmt, ... )
	if s then
		if string.find(fmt, 'q') then
			print(message, e)
		end
		print( message, string.byte(e,1,#e) )
	else
		print( message, 'error', e )
	end
end

testformat('plain %', "%%")
testformat("specials (%s)", "---%s---", " %% \000 \r \n ")
testformat("specials (%q)", "---%q---", " %% \000 \r \n ")
testformat("specials (%q)", "---%q---", "0%%0\0000\r0\n0")
testformat("controls (%q)", "---%q---", ' \a \b \f \t \v \\ ')
testformat("controls (%q)", "---%q---", '0\a0\b0\f0\t0\v0\\0')
testformat("extended (%q)", "---%q---", ' \222 \223 \224 ')
testformat("extended (%q)", "---%q---", '0\2220\2230\2240')
testformat("embedded newlines", "%s\r%s\n%s", '===', '===', '===')

-- format long string
print("this is a %s long string", string.rep("really, ", 30))

local function pc(...)
	local s,e = pcall(...)
	return s and e or 'false-'..type(e)
end

local function strtests(name,func,...)
	print(name, 'good', pc( func, ... ) )
	print(name, 'empty', pc( func ) )
	print(name, 'table', pc( func, {} ) )
	print(name, 'nil', pc( func, nil ) )
end

strtests('lower', string.lower, s )
strtests('upper', string.upper, s )
strtests('reverse', string.reverse, s )
strtests('char', string.char, 92, 60, 61, 93 )
stringdumptest = function() 
	return load(string.dump(function(x) return 'foo->'..x end),'bar')('bat')
end
print( 'string.dump test:', pcall(stringdumptest) )


-- floating point formats (not supported yet)
--[==[
local prefixes = {'','+','-'}
local lengths = {'7','2','0','1',''}
local letters = {'f','e','g'}
local fmt, spec, desc
for i,letter in ipairs(letters) do 
	for k,before in ipairs(lengths) do
		for j,prefix in ipairs(prefixes) do
			spec = '(%'..prefix..before..letter..')'
			fmt = spec..'\t'..spec..'\t'..spec..'\t'..spec..'\t'..spec..'\t'..spec
			print(spec, string.format(fmt, 12.34, -12.34, 1/11, -1/11, 300/11, -300/11) )
			for l,after in ipairs(lengths) do
				spec = '(%'..prefix..before..'.'..after..letter..')'
				fmt = spec..' '..spec..' '..spec..' '..spec..' '..spec..' '..spec
				print(spec, string.format(fmt, 12.34, -12.34, 1/11, -1/11, 300/11, -300/11) )
			end
		end
	end
end
--]==]

local function fmterr(...)
	local r, s = pcall(...)
	if r then
		return s
	else
		s = string.gsub(s, "stdin:%d+:%s*", "")
		return s
	end
end

print(fmterr(string.find, "ab%c)0(", "%"))
print(fmterr(string.find, "ab%c)0(", "("))
print(pcall(string.find, "ab%c)0(", ")"))
