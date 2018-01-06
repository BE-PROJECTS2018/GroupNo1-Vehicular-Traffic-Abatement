import cv2
import numpy as np
import os, glob, json, datetime
from geopy.distance import vincenty
from geo_utils import getScales, scaleToGeo
import timeit

class MyEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        else:
            return super(MyEncoder, self).default(obj)
with open('config.json', 'r') as c:
    config = json.load(c)

start = timeit.default_timer()
count = 0
done = 0
vicinity = 100
images = glob.glob('../ss gen/images/**/*.png')
total = len(images)
for image in images:
    com = done/total*100
    print('Completed [{:-<10}] {:.2f}%'.format('='*(int)(com/10),com), end='\r')
    if os.name == 'nt':
        oppath  = image[image.find('\\')+1:]
        opfname = oppath[oppath.find('\\')+1:]      #filename for output image
        opfldr  = oppath[:oppath.find('\\')]        #folder name for output eg. section1
        image   = image.replace('\\','/')           #making it compatible to open input image
    else:
        oppath = image.replace('../ss gen/images/', '')
        opfname = oppath[oppath.find('/')+1:]      #filename for output image
        opfldr  = oppath[:oppath.find('/')]        #folder name for output eg. section1
    frame   = cv2.imread(image)
    lower_limit = np.array([40,0,0])
    upper_limit = np.array([255,100,100])
    mask = cv2.inRange(frame, lower_limit, upper_limit)
    cv2.bitwise_not(mask,mask)
    frame = cv2.bitwise_and(frame, frame, mask=mask)
    #frame   = cv2.resize(frame, (0,0), fx=0.5, fy=0.5)
    date = datetime.datetime.fromtimestamp(os.path.getmtime(image))
    weekday = int(date.strftime('%w'))
    weekofyear = int(date.strftime('%W'))
    hour = int(date.strftime('%H'))
    min = int(date.strftime('%M'))
    month = int(date.strftime('%m'))
    traffic = []  # {lat, lng, week, hour, t}
    N = config[opfldr]['north']
    E = config[opfldr]['east']
    S = config[opfldr]['south']
    W = config[opfldr]['west']
    [s1, s2] = getScales(N, E, S, W, d1=300, d2=400)
    # resImage = np.zeros((300,400,3), dtype=np.uint8)
    
    #green
    lower_limit = np.array([60,150,100])
    upper_limit = np.array([140,250,170])
    mask = cv2.inRange(frame, lower_limit, upper_limit)
    green = cv2.bitwise_and(frame, frame, mask=mask)
    green = cv2.resize(green, (0,0), fx = 0.5, fy = 0.5)
    gind = np.where((green != (0,0,0)).any(axis=2))
    gdata = []
    g = 0
    for i in range(gind[0].size):
        latI = gind[0][i]
        lngI = gind[1][i]
        [lat, lng] = scaleToGeo(latI, lngI, s1, s2, N, W)
        crop = 0 if g <= vicinity else g-vicinity
        if next((d for d in gdata[crop:] if vincenty((d['lat'], d['lng']), (lat, lng)).m <= 50), None) == None:
            gdata.append({
                'lat':lat, 
                'lng':lng, 
                'weekday': weekday,
                'weekofyear': weekofyear,
                'month': month, 
                'hour': hour,
                'min': min, 
                't':0})
            g += 1
            count += 1
            # resImage[latI][lngI] = [80,202,132]
            # if latI != 299:
            #     resImage[latI + 1][lngI] = [80,202,132]
            # if latI != 0:
            #     resImage[latI - 1][lngI] = [80,202,132]
            # if lngI != 399:
            #     resImage[latI][lngI + 1] = [80,202,132]
            # if lngI != 0:
            #     resImage[latI][lngI - 1] = [80,202,132]
    # orange    
    lower_limit = np.array([0,100,200])
    upper_limit = np.array([50,150,255])
    mask = cv2.inRange(frame, lower_limit, upper_limit)
    orange = cv2.bitwise_and(frame, frame, mask=mask)
    orange = cv2.resize(orange, (0,0), fx = 0.5, fy = 0.5)
    oind = np.where((orange != (0,0,0)).any(axis=2))
    odata = []
    o = 0
    for i in range(oind[0].size):
        latI = oind[0][i]
        lngI = oind[1][i]
        [lat, lng] = scaleToGeo(latI, lngI, s1, s2, N, W)
        crop = 0 if o < vicinity else o - vicinity
        if next((d for d in odata[crop:] if vincenty((d['lat'], d['lng']), (lat, lng)).m <= 50), None) == None:
            odata.append({
                'lat':lat, 
                'lng':lng, 
                'weekday': weekday,
                'weekofyear': weekofyear,
                'month': month, 
                'hour': hour,
                'min': min, 
                't':1})
            count += 1
            o += 1
            # resImage[latI][lngI] = [2,125,240]
            # if latI != 299:
            #     resImage[latI + 1][lngI] = [2,125,240]
            # if latI != 0:
            #     resImage[latI - 1][lngI] = [2,125,240]
            # if lngI != 399:
            #     resImage[latI][lngI + 1] = [2,125,240]
            # if lngI != 0:
            #     resImage[latI][lngI - 1] = [2,125,240]
    # red
    lower_limit = np.array([0,0,200])
    upper_limit = np.array([100,100,255])
    mask = cv2.inRange(frame, lower_limit, upper_limit)
    red = cv2.bitwise_and(frame, frame, mask=mask)
    red = cv2.resize(red, (0,0), fx = 0.5, fy = 0.5)
    rind = np.where((red != (0,0,0)).any(axis=2))
    rdata = []
    r = 0
    for i in range(rind[0].size):
        latI = rind[0][i]
        lngI = rind[1][i]
        [lat, lng] = scaleToGeo(latI, lngI, s1, s2, N, W)
        crop = 0 if r < vicinity else r - vicinity
        if next((d for d in rdata[crop:] if vincenty((d['lat'], d['lng']), (lat, lng)).m <= 50), None) == None:
            rdata.append({
                'lat':lat, 
                'lng':lng, 
                'weekday': weekday,
                'weekofyear': weekofyear,
                'month': month, 
                'hour': hour,
                'min': min, 
                't':2})
            count += 1
            r += 1
            # resImage[latI][lngI] = [0,0,230]
            # if latI != 299:
            #     resImage[latI + 1][lngI] = [0,0,230]
            # if latI != 0:
            #     resImage[latI - 1][lngI] = [0,0,230]
            # if lngI != 399:
            #     resImage[latI][lngI + 1] = [0,0,230]
            # if lngI != 0:
            #     resImage[latI][lngI - 1] = [0,0,230]
    # dark red    
    lower_limit = np.array([19,19,150])
    upper_limit = np.array([80,80,190])
    mask = cv2.inRange(frame, lower_limit, upper_limit)
    dred = cv2.bitwise_and(frame, frame, mask=mask)
    dred = cv2.resize(dred, (0,0), fx = 0.5, fy = 0.5)
    drind = np.where((dred != (0,0,0)).any(axis=2))
    drdata = []
    dr = 0
    for i in range(drind[0].size):
        latI = drind[0][i]
        lngI = drind[1][i]
        [lat, lng] = scaleToGeo(latI, lngI, s1, s2, N, W)
        crop = 0 if dr < vicinity else dr - vicinity
        if next((d for d in drdata[crop:] if vincenty((d['lat'], d['lng']), (lat, lng)).m <= 50), None) == None:
            drdata.append({
                'lat':lat, 
                'lng':lng, 
                'weekday': weekday,
                'weekofyear': weekofyear,
                'month': month, 
                'hour': hour,
                'min': min, 
                't':3})
            count += 1
            dr += 1
            # resImage[latI][lngI] = [19,19,158]
            # if latI != 299:
            #     resImage[latI + 1][lngI] = [19,19,158]
            # if latI != 0:
            #     resImage[latI - 1][lngI] = [19,19,158]
            # if lngI != 399:
            #     resImage[latI][lngI + 1] = [19,19,158]
            # if lngI != 0:
            #     resImage[latI][lngI - 1] = [19,19,158]
    traffic = gdata + odata + rdata + drdata
    # cv2.imshow(opfname, resImage)
    opfldr = os.path.join('json',opfldr)
    opfname = opfname.replace('.png', '.json')
    if not os.path.exists(opfldr):
        os.makedirs(opfldr)
    with open(os.path.join(opfldr, opfname), 'w') as f:
        json.dump(traffic, f, cls=MyEncoder)
    done += 1
    print(count)

com = done/total*100
print('Completed [{:-<10}] {:.2f}%'.format('='*(int)(com/10),com))
print('Total {} tuples recorded'.format(count))
elapsed = (int)(timeit.default_timer() - start)
days = elapsed // (24*60*60)
elapsed = elapsed % (24*60*60)
hours = elapsed // (60*60)
elapsed = elapsed % (60*60)
mins = elapsed // 60
elapsed = elapsed % 60
print('Total {:02d}:{:02d}:{:02d}:{:02d}  elapsed'.format(days, hours, mins, elapsed))
cv2.waitKey(0)
cv2.destroyAllWindows()