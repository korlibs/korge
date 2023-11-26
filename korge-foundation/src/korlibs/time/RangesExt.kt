package korlibs.time

public fun TimeSpan.convertRange(srcMin: TimeSpan, srcMax: TimeSpan, dstMin: TimeSpan, dstMax: TimeSpan): TimeSpan = (dstMin + (dstMax - dstMin) * ((this - srcMin) / (srcMax - srcMin)))
public fun DateTime.convertRange(srcMin: DateTime, srcMax: DateTime, dstMin: DateTime, dstMax: DateTime): DateTime = (dstMin + (dstMax - dstMin) * ((this - srcMin) / (srcMax - srcMin)))
