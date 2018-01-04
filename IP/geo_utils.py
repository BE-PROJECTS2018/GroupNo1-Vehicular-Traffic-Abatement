def getScales(N, E, S, W, d1=600, d2=800):
    #shift origin to N,W
    h = N
    k = W
    n = N - h
    s = S - h
    w = W - k
    e = E - k
    #scale factors
    s1 = d1 / s
    s2 = d2 / e
    return [n, e, s, w, s1, s2] #n,e,s,w not needed really

def scaleToGeo(X, Y, s1, s2, N, W):
    return [X/s1 + N, Y/s2 + W] #scale and move origin back to 0,0

def scaleToCoord(x, y, s1, s2, N, W):
    return [(x-N)*s1, (y-W)*s2] #shift to N,W and scale