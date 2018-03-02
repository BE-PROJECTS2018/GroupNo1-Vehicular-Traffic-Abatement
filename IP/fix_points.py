import cv2
import numpy as np
import os, glob, json, datetime
from geopy.distance import vincenty
from geo_utils import getScales, scaleToGeo
from collections import OrderedDict
from pyexcel_ods import save_data

with open('config.json', 'r') as c:
    config = json.load(c)

vicinity = 13
# count = 0
images = [
    '../ss gen/images/section1/6-20-52.png',
    '../ss gen/images/section2/2018-01-11 4-20-48.png'
]

points = [['latitude', 'longitude']]
for image in images:
    oppath = image.replace('../ss gen/images/', '')
    opfname = oppath[oppath.find('/')+1:]      #filename for output image
    opfldr  = oppath[:oppath.find('/')]        #folder name for output eg. section1
    frame   = cv2.imread(image)
    N = config[opfldr]['north']
    E = config[opfldr]['east']
    S = config[opfldr]['south']
    W = config[opfldr]['west']
    [s1, s2] = getScales(N, E, S, W, d1=600, d2=800)
    # resImage = np.zeros((600,800,3), dtype=np.uint8)
    #all
    lower_limit = np.array([32,0,0])
    upper_limit = np.array([255,0,0])
    mask = cv2.inRange(frame, lower_limit, upper_limit)
    cv2.bitwise_not(mask,mask)
    all = cv2.bitwise_and(frame, frame, mask=mask)
    # all = cv2.resize(all, (0,0), fx = 0.5, fy = 0.5)
    aind = np.where((all != (0,0,0)).any(axis=2))
    adata = np.full((600,800), None)
    for i in range(aind[0].size):
        latI = aind[0][i]
        lngI = aind[1][i]
        [lat, lng] = scaleToGeo(latI, lngI, s1, s2, N, W)
        if latI < vicinity:
            xS = 0
        else:
            xS = latI - vicinity
        if latI == 599:
            xE = 600
        else:
            xE = latI + 1
        
        if lngI < vicinity:
            yS = 0
        else:
            yS = lngI - vicinity
        if 799 - lngI < vicinity:
            yE = 800
        else:
            yE = lngI + vicinity
        if next((d for d in adata[xS:xE, yS:yE].flatten().tolist() if d != None and vincenty((d['lat'], d['lng']), (lat, lng)).m <= 50), None) == None:
            adata[latI][lngI] = {
                'lat':lat, 
                'lng':lng, 
                }
            points.append([lat, lng])
    #         count += 1
    #         resImage[latI][lngI] = [80,202,132]
    #         if latI != 599:
    #             resImage[latI + 1][lngI] = [80,202,132]
    #         if latI != 0:
    #             resImage[latI - 1][lngI] = [80,202,132]
    #         if lngI != 799:
    #             resImage[latI][lngI + 1] = [80,202,132]
    #         if lngI != 0:
    #             resImage[latI][lngI - 1] = [80,202,132]
    # cv2.imshow(opfname, resImage)
# print(count)
csv = OrderedDict([('', points)])
save_data('points.csv', csv)
cv2.destroyAllWindows()
    