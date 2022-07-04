local t = {}
local template = [[
	local f<i>
	do
		local result
		function f<i>()
			if not result then
				result = f<i-2>() + f<i-1>()
			end
			return result
		end
	end
]]
t[1] = [[
	local f1
	f1 = function() return 1 end
]]
t[2] = [[
	local f2
	f2 = function() return 1 end
]]
for i = 3, 199 do
	t[i] = template:gsub("<([^>]+)>", function(s)
		local c = assert(load('return '..s, 'f'..i, 'bt', { i = i }), 'could not compile: '..s)
		return c()
	end)
end
t[200] = [[
	print("5th fibonacci number is", f5())
	print("10th fibonacci number is", f10())
	print("199th fibonacci number is", f199())
]]

local s = table.concat(t)
print(s)
f = load(s)
f()
