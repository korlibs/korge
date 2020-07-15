-- The Computer Language Shootout
-- http://shootout.alioth.debian.org/
-- contributed by Isaac Gouy
-- modified by Mike Pall


local function nsieve(m,isPrime)
   for i=2,m do
      isPrime[i] = true
   end
   local count = 0

   for i=2,m do
      if isPrime[i] then
         for k=i+i, m, i do
            if isPrime[k] then isPrime[k] = false end
         end
         count = count + 1
      end
   end
   return count
end


local n = tonumber(arg and arg[1]) or 1
local flags = {}

local m = (2^n)*10000
print( string.format("Primes up to %8d %8d", m, nsieve(m,flags)))

m = (2^(n-1))*10000
print( string.format("Primes up to %8d %8d", m, nsieve(m,flags)))

m = (2^(n-2))*10000
print( string.format("Primes up to %8d %8d", m, nsieve(m,flags)))
