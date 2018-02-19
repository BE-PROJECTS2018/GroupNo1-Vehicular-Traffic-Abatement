import cv2
import numpy as np
import os, glob, datetime
from pyexcel_ods import save_data
from collections import OrderedDict
import timeit

start = timeit.default_timer()
count = 0
done = 0
err = 0
images = glob.glob('../ss gen/images/**/*.png')
total = len(images)
traffic = [['latitude', 'longitude', 'weekday', 'hour', 'min', 'green', 'orange', 'red', 'darkred']]
print('Grouping images...', end='', flush=True)
grouped=[[],[],[],[],[],[],[]] #sun-sat
for i in range(7):
    for j in range(96): # 96 readings in a day
        grouped[i].append([])
for image in images:
    date = datetime.datetime.fromtimestamp(os.path.getmtime(image))
    weekday = int(date.strftime('%w'))
    hour = int(date.strftime('%H'))
    min = int(date.strftime('%M'))
    grouped[weekday][(hour*60+min)//15].append(image)
print('done')
group_factor = 8
width = 600//group_factor
height = 800//group_factor
for i in range(7):
    for j in range(96):
        images = grouped[i][j]
        hour = (j*15)//60
        min = j*15 - hour*60
        weekday = i
        gdata = np.full((width,height), 0)
        odata = np.full((width,height), 0)
        rdata = np.full((width,height), 0)
        drdata = np.full((width,height), 0)
        for image in images:
            com = done/total*100
            print('\rCompleted [{:-<10}] {:.2f}% {}'.format('='*(int)(com/10),com, image), end='')
            try:
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
                #green
                lower_limit = np.array([60,150,100])
                upper_limit = np.array([140,250,170])
                mask = cv2.inRange(frame, lower_limit, upper_limit)
                green = cv2.bitwise_and(frame, frame, mask=mask)
                gind = np.where((green != (0,0,0)).any(axis=2))
                for x in range(gind[0].size):
                    h = gind[0][x] // group_factor
                    k = gind[1][x] // group_factor
                    gdata[h][k] += 1
                # orange    
                lower_limit = np.array([0,100,200])
                upper_limit = np.array([70,160,255])
                mask = cv2.inRange(frame, lower_limit, upper_limit)
                orange = cv2.bitwise_and(frame, frame, mask=mask)
                oind = np.where((orange != (0,0,0)).any(axis=2))
                for x in range(oind[0].size):
                    h = oind[0][x] // group_factor
                    k = oind[1][x] // group_factor
                    odata[h][k] += 1
                # red
                lower_limit = np.array([0,0,200])
                upper_limit = np.array([100,75,255])
                mask = cv2.inRange(frame, lower_limit, upper_limit)
                red = cv2.bitwise_and(frame, frame, mask=mask)
                rind = np.where((red != (0,0,0)).any(axis=2))
                for x in range(rind[0].size):
                    h = rind[0][x] // group_factor
                    k = rind[1][x] // group_factor
                    rdata[h][k] += 1
                # dark red    
                lower_limit = np.array([19,19,150])
                upper_limit = np.array([80,80,190])
                mask = cv2.inRange(frame, lower_limit, upper_limit)
                dred = cv2.bitwise_and(frame, frame, mask=mask)
                drind = np.where((dred != (0,0,0)).any(axis=2))
                for x in range(drind[0].size):
                    h = drind[0][x] // group_factor
                    k = drind[1][x] // group_factor
                    drdata[h][k] += 1
            except Exception:
                err += 1
            done += 1
        # div_factor = group_factor * group_factor * len(images)
        for x in range(width):
            for y in range(height):
                if gdata[x][y] != 0 or odata[x][y] != 0 or rdata[x][y] != 0 or drdata[x][y] != 0:
                    div_factor = gdata[x][y] + odata[x][y] + rdata[x][y] + drdata[x][y]
                    record = [x, y, weekday, hour, min,
                            gdata[x][y]/div_factor,
                            odata[x][y]/div_factor,
                            rdata[x][y]/div_factor,
                            drdata[x][y]/div_factor]
                    count += 1
                    traffic.append(record)
com = done/total*100
print('\rCompleted [{:-<10}] {:.2f}%'.format('='*(int)(com/10),com))
print('Total {} tuples recorded'.format(count))
print('{} errored images'.format(err))
data = OrderedDict([('', traffic)])
if not os.path.exists('data'):
    os.makedirs(opfldr)
save_data('v2.csv', data)
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